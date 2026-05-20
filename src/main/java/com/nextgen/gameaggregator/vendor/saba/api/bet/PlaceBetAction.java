package com.nextgen.gameaggregator.vendor.saba.api.bet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawSportsBetDetailsProducer;
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
    private final RawSportsBetDetailsProducer rawSportsBetDetailsProducer;

    private static final String VENDOR = "saba";
    private static final String EVENT_FAMILY = "placebet";

    public PlaceBetAction(GameSessionService gameSessionService,
                          HttpService httpService,
                          SportWalletService sportWalletService,
                          WalletRequestService walletRequestService,
                          RedissonService redissonService,
                          RawSportsBetDetailsProducer rawSportsBetDetailsProducer) {

        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
        this.sportWalletService = sportWalletService;
        this.walletRequestService = walletRequestService;
        this.redissonService = redissonService;
        this.rawSportsBetDetailsProducer = rawSportsBetDetailsProducer;
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

            this.emitRawBetDetail(walletRequest, dto.getMessage(), httpRequestLog.getRequestBody());

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

    private void emitRawBetDetail(WalletRequest walletRequest, PlaceBetDto dto, String requestBody) {
        try {
            if (dto == null || dto.getRefId() == null) {
                log.warn("Skipping SABA placebet emit: missing refId traceId={}", walletRequest == null ? null : walletRequest.getTraceId());
                return;
            }
            rawSportsBetDetailsProducer.emit(BetDetailEmitRequest.builder()
                    .vendor(VENDOR)
                    .eventFamily(EVENT_FAMILY)
                    .eventKind(EventKind.PLACE_BET)
                    .vendorBetId(dto.getRefId())
                    .gaBetId(walletRequest.getBetId())
                    .roundId(dto.getRefId())
                    .vendorPlayerUsername(walletRequest.getVendorPlayerUsername())
                    .agentId(walletRequest.getAgentId())
                    .requestBody(requestBody)
                    .build());
        } catch (Exception e) {
            // emit-only — never block the wallet path
            log.warn("SABA placebet emit failed refId={}: {}", dto == null ? null : dto.getRefId(), e.getMessage());
        }
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
        walletRequest.setOperatorTimeoutTiming(EndPoints.BET_TIMEOUT);
    }
}
