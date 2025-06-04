package com.nextgen.gameaggregator.vendor.tbp.api.withdraw;

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
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.tbp.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class WithdrawAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public WithdrawAction(WalletService walletService,
                          HttpService httpService,
                          ValidationService validationService,
                          VendorService vendorService,
                          GameSessionService gameSessionService,
                          RequestIdempotentLogService requestIdempotentLogService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.WITHDRAW)
    public WithdrawVo betAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        WithdrawVo responseVo = new WithdrawVo();
        WithdrawDto dto = new WithdrawDto();
        BigDecimal balance;
        GameSession gameSession = new GameSession();
        boolean isRequestExists = false;

        try {
            dto = HttpService.convertJsonToDto(body, WithdrawDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto, dto.getPlayerId()) == null) {
                requestIdempotentLogService.create(dto, dto.getPlayerId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Get GameSession with username
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());

            // Verify parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            //Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
            balance = betEvent.getLastBalance();

            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setError(ResponseCode.OK);

        } catch (Exception e) {
            this.handleException(e, responseVo, dto, gameSession, httpRequestLog);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getUsername());
            }
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(WithdrawDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        if (dto.getPlatformType() != null && dto.getPlatformType().trim().isEmpty()) {
            throw new InvalidRequestException("PlatformType field is Mandatory.");
        }
    }

    private void doVerification(WithdrawDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException {
        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getPlayerId());

        //2. verify Username, Password, PlayerId, Currency, SessionId
        vendorService.validate(dto.getUsername(), dto.getPassword(), dto.getPlayerId(), dto.getCurrency(), dto.getSessionId(), gameSession);

        //3. Verify GameNumber
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameNumber(), AuthenticationException::new);

    }

    @ExceptionHandler({
            BetResultIdempotentViolationException.class,
            InsufficientBalanceException.class,
            InvalidRequestException.class,
            AuthenticationException.class,
            Exception.class
    })
    private void handleException(Exception e, WithdrawVo responseVo, WithdrawDto dto, GameSession gameSession, HttpRequestLog httpRequestLog) {

        if (e instanceof BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setError(ResponseCode.OK);
        } else if (e instanceof InsufficientBalanceException) {
            responseVo.setError(ResponseCode.INSUFFICIENT_FUNDS);
        } else if (e instanceof InvalidRequestException) {
            responseVo.setError(ResponseCode.UNEXPECTED_INPUT);
        } else if (e instanceof AuthenticationException) {
            responseVo.setError(ResponseCode.PERMISSION_DENIED);
        } else {
            responseVo.setError(ResponseCode.INTERNAL_SERVER_ERROR);
        }

        httpService.logError(httpRequestLog, e);
    }
}
