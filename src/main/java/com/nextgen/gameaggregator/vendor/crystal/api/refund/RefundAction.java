package com.nextgen.gameaggregator.vendor.crystal.api.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.crystal.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.crystal.service.VendorService;
import com.nextgen.gameaggregator.vendor.crystal.vo.CommonDataVo;
import com.nextgen.gameaggregator.vendor.crystal.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RefundAction {
    private final HttpService httpService;
    private final VendorService vendorService;
    private final WalletService walletService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public RefundAction(HttpService httpService,
                        VendorService vendorService,
                        WalletService walletService,
                        RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.walletService = walletService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.REFUND)
    public CommonDataVo rollback(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        RefundDto refundDto = new RefundDto();
        CommonDataVo commonDataVo = new CommonDataVo();
        BigDecimal balance;
        boolean isRequestExists = false;
        try {
            String body = httpRequestLog.getRequestBody();
            refundDto = HttpService.convertJsonToDto(body, RefundDto.class);
            VendorService.doValidation(refundDto);

            if (requestIdempotentLogService.checkExists(refundDto, refundDto.getPlayerId()) == null) {
                requestIdempotentLogService.create(refundDto, refundDto.getPlayerId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            GameSession gameSession = vendorService.checkGameSession(traceId, refundDto.getPlayerId(), refundDto.getGameCode());
            vendorService.doCompareSignature(request, httpRequestLog, gameSession);
            this.doVerification(refundDto.getGameCode(), refundDto.getCurrencyCode(), gameSession);

            balance = walletService.processRollback(traceId, refundDto, gameSession, vendorService, httpRequestLog);

            commonDataVo = vendorService.prepareVo(balance, refundDto.getTransactionId());

        } catch (BetResultIdempotentViolationException e) {
            commonDataVo = vendorService.prepareVo(e.getBalance(), refundDto.getTransactionId());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            this.handleException(e, commonDataVo, httpRequestLog);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(refundDto, refundDto.getPlayerId());
            }
            httpService.end(httpRequestLog, commonDataVo);
        }
        return commonDataVo;
    }

    private void doVerification(String gameId, String currency, GameSession gameSession)
            throws DisabledAgentPlayerException,
            DisabledVendorLineException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

        vendorService.validate(currency, gameSession);
        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), gameId, GameNotSupportedException::new);
    }

    @ExceptionHandler({InvalidRequestException.class, InvalidPlayerException.class,
            AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonDataVo commonDataVo, HttpRequestLog httpRequestLog) {

        if (e instanceof InvalidRequestException) {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.INVALID_PARAMETERS.code,
                    ResponseCodes.INVALID_PARAMETERS.message
            ));
        } else if (e instanceof AuthenticationException) {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.INVALID_SIGNATURE.code,
                    ResponseCodes.INVALID_SIGNATURE.message
            ));
        } else if (e instanceof BetNotFoundException) {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.TRANSACTION_NOT_FOUND.code,
                    ResponseCodes.TRANSACTION_NOT_FOUND.message
            ));
        } else {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.PLAYER_NOT_FOUND.code,
                    ResponseCodes.PLAYER_NOT_FOUND.message
            ));
        }
        commonDataVo.setData(null);
        httpService.logError(httpRequestLog, e);
    }
}