package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class CashInAction {
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public CashInAction(HttpService httpService,
                        WalletService walletService,
                        VendorService vendorService,
                        RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.CASHIN)
    public CommonVo betAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String jwtAuth = request.getHeader("Authorization");
        CommonVo responseVo = new CommonVo();
        CashInDto dto = new CashInDto();
        BigDecimal balance;
        GameSession gameSession = null;
        boolean isRequestExists = false;

        try {
            dto = HttpService.convertJsonToDto(body, CashInDto.class);
            dto.setAuthorization(jwtAuth);

            //Add request header log
            httpRequestLog.setRequestBody("Request Body: \n" + body + "\n\nRequest Header: \n" + vendorService.getHeaders(request));

            // Validate request parameters from vendor (Non-database related)
            VendorService.doValidation(dto);

            // Request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto, dto.getRoundId()) == null) {
                requestIdempotentLogService.create(dto, dto.getRoundId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Get GameSession with Token
            gameSession = vendorService.checkGameSession(traceId, VendorService.jwtGetUserId(jwtAuth), dto.getGameId(), dto.getSessionId());

            // Verify parameters (Verify against database values)
            this.doVerification(jwtAuth, dto, gameSession);

            // If reason is REVERSE_FUND process refund
            if (dto.getReason().equals("REVERSE_FUND")) {
                ReverseCashInDto reverseDto = new ReverseCashInDto();
                reverseDto.setPreviousTransactionId(dto.getPreviousTransactionId());
                balance = walletService.processRollback(traceId, reverseDto, gameSession, vendorService, httpRequestLog);

                //Else Process settle
            } else {
                // Calculate winAmount and JackpotAmount
                ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), false);

                // Process settlement
                balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            }


            responseVo.setResponseSuccess(balance, gameSession.getVendorPlayerId().toString(), gameSession.getVendorPlayerUsername());

        } catch (Exception e) {
            this.handleException(e, responseVo, gameSession, httpRequestLog);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getRoundId());
            }
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doVerification(String jwtAuth, CashInDto dto, GameSession gameSession) throws
            AuthenticationException,
            GameNotSupportedException,
            InvalidCurrencyException,
            CredentialNotFoundException {

        //Verify JWT
        vendorService.verifyJWT(jwtAuth, gameSession.getVendorLineId(), gameSession.getVendorPlayerUsername());

        //Check vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);

        //Check vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), InvalidCurrencyException::new);

    }

    @ExceptionHandler({
            BetResultIdempotentViolationException.class,
            InsufficientBalanceException.class,
            AuthenticationException.class,
            Exception.class
    })
    private void handleException(Exception e, CommonVo responseVo, GameSession gameSession, HttpRequestLog httpRequestLog) {

        if (e instanceof BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setResponseSuccess(betResultIdempotentViolationException.getBalance(),
                    gameSession.getVendorPlayerId().toString(), gameSession.getVendorPlayerUsername());

        } else if (e instanceof AuthenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTH_ERROR);
        } else {
            responseVo.setResponseCode(ResponseCode.SERVER_ERROR);
        }

        httpService.logError(httpRequestLog, e);
    }
}
