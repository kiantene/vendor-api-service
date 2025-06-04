package com.nextgen.gameaggregator.vendor.tbp.api.deposit;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
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
public class DepositAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public DepositAction(WalletService walletService,
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

    @PostMapping(path = EndPoints.DEPOSIT)
    public DepositVo settleAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        DepositVo responseVo = new DepositVo();
        DepositDto dto = new DepositDto();
        BigDecimal balance;
        GameSession gameSession = new GameSession();
        boolean isRequestExists = false;

        try {
            dto = HttpService.convertJsonToDto(body, DepositDto.class);

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
            vendorService.checkGameSession(traceId, dto.getPlayerId(), dto.getGameNumber(), dto.getSessionId());

            // Verify parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            //Settle
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), false);

            balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
            responseVo.setError(ResponseCode.OK);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setBalance(e.getBalance().setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
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

    private void doValidation(DepositDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        if (dto.getPlatformType() != null && dto.getPlatformType().trim().isEmpty()) {
            throw new InvalidRequestException("PlatformType field is Mandatory.");
        }
    }

    private void doVerification(DepositDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException {
        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getPlayerId());

        //2. verify Username, Password, PlayerId
        vendorService.validate(dto.getUsername(), dto.getPassword(), dto.getPlayerId(), gameSession);

        //3. Verify GameNumber
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameNumber(), AuthenticationException::new);

        //4. Verify SessionId
        ValidationUtils.isEquals(gameSession.getVendorToken(), dto.getSessionId(), AuthenticationException::new);

        //5. Verify Currency
        ValidationUtils.isEquals(gameSession.getCurrencyCode(), dto.getCurrency(), AuthenticationException::new);
    }
}
