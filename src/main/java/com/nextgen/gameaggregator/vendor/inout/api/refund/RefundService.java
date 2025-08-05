package com.nextgen.gameaggregator.vendor.inout.api.refund;


import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
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
    private final GameSessionService gameSessionService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public RefundService(HttpService httpService,
                         WalletService walletService,
                         VendorService vendorService,
                         VendorLineService vendorLineService,
                         GameSessionService gameSessionService,
                         RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.gameSessionService = gameSessionService;
    }

    public CommonVo refund(HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
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
            if (requestIdempotentLogService.checkExists(dto.getData(), dto.getData().getUser_id()) == null) {
                requestIdempotentLogService.create(dto.getData(), dto.getData().getUser_id());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            BigDecimal balance = walletService.processRollback(traceId, dto.getData(), gameSession, vendorService, httpRequestLog);

            // 5. Set response data
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(balance.toString());
        } catch (Exception e) {
            this.handleException(e, responseVo, httpRequestLog);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto.getData(), dto.getData().getUser_id());
            }
            httpService.end(httpRequestLog, responseVo);
        }


        return responseVo;
    }

    private void doValidation(CommonDto<RefundDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto<RefundDto> dto, GameSession gameSession) throws
            AuthenticationException, CredentialNotFoundException {
        // 1. Verify GameMode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameMode(), AuthenticationException::new);

        // 2. Verify UserId
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getData().getUser_id(), AuthenticationException::new);

        // 3. Verify Currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getData().getCurrency(), AuthenticationException::new);

        // 4. Verify OperatorId
        String operatorId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_ID);
        ValidationUtils.isEquals(operatorId, dto.getData().getOperator(), AuthenticationException::new);
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
