package com.nextgen.gameaggregator.vendor.tbp.api.withdraw;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.Credentials;
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.tbp.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public WithdrawAction(WalletService walletService,
                          HttpService httpService,
                          ValidationService validationService,
                          VendorService vendorService,
                          VendorLineService vendorLineService,
                          GameSessionService gameSessionService,
                          RequestIdempotentLogService requestIdempotentLogService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
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
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameNumber(), gameSession);

            // Verify parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            //Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
            balance = betEvent.getLastBalance();

            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setError(ResponseCode.OK);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setBalance(e.getBalance().setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setError(ResponseCode.OK);

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseCode.INSUFFICIENT_FUNDS);

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseCode.UNEXPECTED_INPUT);

        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseCode.PERMISSION_DENIED);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseCode.INTERNAL_SERVER_ERROR);
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
    }

    private void doVerification(WithdrawDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException {
        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getPlayerId());

        //2. Verify Username
        String username = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        ValidationUtils.isEquals(username, dto.getUsername(), AuthenticationException::new);

        //3. Verify Password
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.TOKEN);
        ValidationUtils.isEquals(password, dto.getPassword(), AuthenticationException::new);

        //4. Verify PlayerId
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), AuthenticationException::new);

        //5. Verify GameNumber
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameNumber(), AuthenticationException::new);

        //6. Verify SessionId
        ValidationUtils.isEquals(gameSession.getVendorToken(), dto.getSessionId(), AuthenticationException::new);

        //7. Verify Currency
        ValidationUtils.isEquals(gameSession.getCurrencyCode(), dto.getCurrency(), AuthenticationException::new);
    }
}
