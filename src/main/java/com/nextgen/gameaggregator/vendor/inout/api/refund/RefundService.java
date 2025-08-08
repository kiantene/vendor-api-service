package com.nextgen.gameaggregator.vendor.inout.api.refund;


import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.service.VendorService;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;

@Service
public class RefundService {
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final UnsettledBetService unsettledBetService;

    public RefundService(HttpService httpService,
                         WalletService walletService,
                         VendorService vendorService,
                         VendorLineService vendorLineService,
                         RequestIdempotentLogService requestIdempotentLogService,
                         UnsettledBetService unsettledBetService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.unsettledBetService = unsettledBetService;
    }

    public CommonVo refund(HttpRequestLog httpRequestLog, String xSign) {
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        BigDecimal balance = BigDecimal.ZERO;
        UnsettledBet unsettledBet = new UnsettledBet();
        CommonDto<RefundDto> dto = new CommonDto<>();
        CommonVo responseVo = new CommonVo();
        boolean isRequestExists = false;

        try {
            // 1. Retrieve request body and convert into dto
            dto = HttpService.convertJsonToDto(body, new TypeReference<>() {
            });

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto.getData(), dto.getData().getUserId()) == null) {
                requestIdempotentLogService.create(dto.getData(), dto.getData().getUserId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 3. Verify session token
            GameSession gameSession = vendorService.checkGameSession(traceId, dto.getData().getUserId(), dto.getGameMode(), dto.getToken());

            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Verify remaining parameters (Verify against database values)
            vendorService.doVerification(dto.getData().getCurrency(), dto.getGameMode(), dto.getData().getUserId(), gameSession, secretKey, body, xSign);

            try {
                unsettledBet = unsettledBetService.findBetsForRollback(gameSession.getVendorPlayerId(), dto.getData().getDebitId());
            } catch (BetNotFoundException betNotFoundException) {
                balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
            }

            if (unsettledBet != null) {
                balance = walletService.processRollback(traceId, dto.getData(), gameSession, vendorService, httpRequestLog);
            }
            // 5. Set response data
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(balance.toString());

        } catch (Exception e) {
            this.handleException(e, responseVo, httpRequestLog);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto.getData(), dto.getData().getUserId());
            }
        }
        return responseVo;
    }

    private void doValidation(CommonDto<RefundDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {
        if (e instanceof InvalidRequestException) {
            responseVo.setError(ResponseCode.INVALID_TOKEN);
        } else if (e instanceof AuthenticationException) {
            responseVo.setError(ResponseCode.ACCOUNT_LOCKED);
        } else {
            responseVo.setError(ResponseCode.UNKNOWN_ERROR);
        }

        httpService.logError(httpRequestLog, e);
    }
}
