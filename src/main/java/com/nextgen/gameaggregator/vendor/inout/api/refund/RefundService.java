package com.nextgen.gameaggregator.vendor.inout.api.refund;


import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
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

    public CommonVo refund(HttpRequestLog httpRequestLog, String xSign) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        BigDecimal balance = BigDecimal.ZERO;
        CommonDto<RefundDto> dto = new CommonDto<>();
        CommonVo responseVo = new CommonVo();
        boolean isRequestExists = false;
        GameSession gameSession = null;

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
            gameSession = vendorService.checkGameSession(traceId, dto.getData().getUserId(), dto.getGameMode(), dto.getToken());

            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Verify remaining parameters (Verify against database values)
            vendorService.doVerification(dto.getData().getCurrency(), dto.getGameMode(), dto.getData().getUserId(), gameSession, secretKey, body, xSign);

            // 5. Check and Process Rollback
            balance = checkRollback(traceId, dto, gameSession, httpRequestLog);

            // 6. Set response data
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(balance.toString());

        } catch (BetRefundIdempotentViolationException | BetResultIdempotentViolationException e ) {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(balance.toString());
            httpService.logError(httpRequestLog, e);

        }  catch (Exception e) {
            this.handleException(e, responseVo, httpRequestLog);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto.getData(), dto.getData().getUserId());
            }
        }
        return responseVo;
    }

    private BigDecimal checkRollback(String traceId, CommonDto<RefundDto> dto, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException, BetRefundIdempotentViolationException, TransactionStillProcessingException, InvalidOperatorResponseException, BetNotFoundException, InvalidFormatException {
        UnsettledBet unsettledBet = new UnsettledBet();
        BigDecimal balance = BigDecimal.ZERO;
        try {
            unsettledBet = unsettledBetService.findBetsForRollback(gameSession.getVendorPlayerId(), dto.getData().getDebitId());
        } catch (BetNotFoundException | TransactionStillProcessingException e) {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
        }

        if (unsettledBet != null) {
            balance = walletService.processRollback(traceId, dto.getData(), gameSession, vendorService, httpRequestLog);
        }
        return balance;
    }

    private void doValidation(CommonDto<RefundDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    @ExceptionHandler({InvalidRequestException.class,
            AuthenticationException.class,
            DisabledVendorLineException.class,
            Exception.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {
        vendorService.exceptionHandler(e, responseVo);
        httpService.logError(httpRequestLog, e);
    }
}
