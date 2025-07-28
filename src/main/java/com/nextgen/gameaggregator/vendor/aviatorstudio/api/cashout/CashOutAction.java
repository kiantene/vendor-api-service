package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashout;


import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class CashOutAction {
    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public CashOutAction(GameSessionService gameSessionService,
                         HttpService httpService,
                         WalletService walletService,
                         VendorService vendorService,
                         ValidationService validationService,
                         RequestIdempotentLogService requestIdempotentLogService) {
        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.validationService = validationService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.CASHOUT)
    public ResponseEntity<CommonVo> betAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String jwtAuth = request.getHeader("Authorization");
        CommonVo responseVo = new CommonVo();
        CashOutDto dto = new CashOutDto();
        BigDecimal balance;
        GameSession gameSession = null;
        boolean isRequestExists = false;
        HttpStatus status = HttpStatus.OK;

        try {
            dto = HttpService.convertJsonToDto(body, CashOutDto.class);
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
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(VendorService.jwtGetUserId(jwtAuth));

            // Verify parameters (Verify against database values)
            this.doVerification(jwtAuth, dto, gameSession);

            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
            balance = betEvent.getLastBalance();

            responseVo.setResponseSuccess(balance, gameSession.getVendorPlayerId().toString(), gameSession.getVendorPlayerUsername());

        } catch (Exception e) {
            status = this.handleException(e, responseVo, gameSession, httpRequestLog);
            httpService.logError(httpRequestLog, e);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getRoundId());
            }
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, new HttpHeaders(), status);
    }

    private void doVerification(String jwtAuth, CashOutDto dto, GameSession gameSession) throws
            InvalidPlayerException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException,
            GameNotSupportedException,
            InvalidCurrencyException,
            CredentialNotFoundException {

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

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
    private HttpStatus handleException(Exception e, CommonVo responseVo, GameSession gameSession, HttpRequestLog httpRequestLog) {

        if (e instanceof BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setResponseSuccess(betResultIdempotentViolationException.getBalance(),
                    gameSession.getVendorPlayerId().toString(), gameSession.getVendorPlayerUsername());
            return HttpStatus.OK;
        } else if (e instanceof InsufficientBalanceException) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_FUNDS);
            return HttpStatus.BAD_REQUEST;
        } else if (e instanceof AuthenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTH_ERROR);
            return HttpStatus.FORBIDDEN;
        } else {
            responseVo.setResponseCode(ResponseCode.SERVER_ERROR);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}