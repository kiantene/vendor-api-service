package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.service.BetIdempotentLogService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RedissonService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = EndPoints.PATH)
public class CancelBetAction {

    private final HttpService httpService;
    private final SportWalletService sportWalletService;
    private final WalletRequestService walletRequestService;
    private final BetIdempotentLogService betIdempotentLogService;
    private final RedissonService redissonService;

    @Autowired
    public CancelBetAction(HttpService httpService,
                           SportWalletService sportWalletService,
                           WalletRequestService walletRequestService,
                           BetIdempotentLogService betIdempotentLogService,
                           RedissonService redissonService) {

        this.httpService = httpService;
        this.sportWalletService = sportWalletService;
        this.walletRequestService = walletRequestService;
        this.betIdempotentLogService = betIdempotentLogService;
        this.redissonService = redissonService;
    }

    @PostMapping(path = EndPoints.CANCEL_BET)
    public GeneralVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        // Construct Vo
        GeneralVo vo = new GeneralVo();
        RLock rLock;

        try {
            RequestDto<CancelBetDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            CancelBetDto cancelBetDto = dto.getMessage();
            final String vendorPlayerUsername = cancelBetDto.getUserId();
            final String operationId = cancelBetDto.getOperationId();

            // check operationId for idempotent and throw error to vendor
            String idempotentKey = vendorPlayerUsername + "_" + operationId;
            betIdempotentLogService.idempotentCheck(idempotentKey);

            walletRequestService.updateByVendorUsername(walletRequest, vendorPlayerUsername);
            walletRequest.setTimestamp(cancelBetDto.getTimestamp());

            for (CancelBetTransactionDto txn : cancelBetDto.getTxns()) {
                final String refId = txn.getRefId();
                final String externalTransactionId = VendorService.generateExtTxnId(operationId, refId);
                final WalletRequest newWalletRequest = new WalletRequest(walletRequest);

                String redisKey = "RedissonLock:Saba" + refId;
                rLock = redissonService.getLock(redisKey); //check for lock
                if (rLock.remainTimeToLive() != -2) { // time in milliseconds -2 if the lock does not exist. -1 if the lock exists but has no associated expire.
                    redissonService.waitUntilLockReleased(rLock, httpRequestLog); //loop until lock is released
                } //else is not exist, proceed
                newWalletRequest.setExternalTransactionId(externalTransactionId);
                newWalletRequest.setVendorBetId(refId);


                SportWalletService.THREAD_POOL.submit(() -> {
                    try {
                        sportWalletService.refund(newWalletRequest);

                    } catch (Exception exception) {
                        // throw to dlq
                        log.error(exception.getMessage());
                    } finally {
                        walletRequestService.end(newWalletRequest, httpRequestLog, vo);
                    }
                });
            }

        } catch (JsonProcessingException |
                 InvalidPlayerException |
                 BetNotAllowedException exception) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, exception);
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        } catch (DuplicateRequestException duplicateRequestException) {
            httpService.logError(httpRequestLog, duplicateRequestException);
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);
            walletRequestService.end(walletRequest, httpRequestLog, vo);
        }

        return vo;
    }
}
