package com.nextgen.gameaggregator.vendor.saba.api.bet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RedissonService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class PlaceBetAction {

    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final SportWalletService sportWalletService;
    private final WalletRequestService walletRequestService;
    private final RedissonService redissonService;

    public PlaceBetAction(GameSessionService gameSessionService,
                          HttpService httpService,
                          SportWalletService sportWalletService,
                          WalletRequestService walletRequestService,
                          RedissonService redissonService) {

        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
        this.sportWalletService = sportWalletService;
        this.walletRequestService = walletRequestService;
        this.redissonService = redissonService;
    }

    @PostMapping(path = EndPoints.PLACE_BET)
    public PlaceBetVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        String traceId = walletRequest.getTraceId();

        // Construct Vo
        PlaceBetVo vo = new PlaceBetVo();
        RLock rLock = null;

        try {
            // Convert original request body into dto
            RequestDto<PlaceBetDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            String redisKey = "RedissonLock:Saba" + dto.getMessage().getRefId();
            rLock = redissonService.getLock(redisKey);
            // Try to acquire the lock with a lease time of 100 MILLISECONDS
            if (rLock.remainTimeToLive() != -2) { // time in milliseconds -2 if the lock does not exist. -1 if the lock exists but has no associated expire.
                throw new TransactionStillProcessingException();
            }

            rLock.lock(5000, TimeUnit.MILLISECONDS);


            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getUserId());
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            this.dataMapper(walletRequest, dto.getMessage());


            walletRequest = sportWalletService.placeBet(walletRequest);

            vo.setResponseCode(ResponseCode.SUCCESS);
            vo.setRefId(walletRequest.getRoundId());
            vo.setLicenseeTxId(traceId);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
            httpService.logError(httpRequestLog, e);

        } catch (InsufficientBalanceException e) {
            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);
            walletRequest.setErrorMessage(e.getMessage());

        } finally {
            redissonService.deleteLockSafely(rLock);
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        }

        return vo;
    }

    private void dataMapper(WalletRequest walletRequest, PlaceBetDto placeBetDto) {
        String externalTransactionId = VendorService.generateExtTxnId(placeBetDto.getOperationId(), placeBetDto.getRefId());

        walletRequest.setExternalTransactionId(externalTransactionId);
        walletRequest.setVendorBetId(placeBetDto.getRefId());
        walletRequest.setRoundId(placeBetDto.getRefId());
        walletRequest.setBetAmount(placeBetDto.getActualAmount());
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setBetStatus(BetStatus.UNSETTLED);
        walletRequest.setBetType(BetType.NORMAL_BET.code);
        walletRequest.setVendorPlayerUsername(placeBetDto.getUserId());
    }
}
