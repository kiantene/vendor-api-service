package com.nextgen.gameaggregator.vendor.playtech.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playtech.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playtech.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playtech.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonBalanceVo;
import com.nextgen.gameaggregator.vendor.playtech.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetAction {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final ValidationService validationService;

    @Autowired
    public BetAction(HttpService httpService,
                     ValidationService validationService,
                     WalletService walletService,
                     VendorService vendorService,
                     GameSessionService gameSessionService,
                     VendorLineService vendorLineService,
                     AgentPlayerService agentPlayerService) {
        this.validationService = validationService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
    }

    @PostMapping(path = EndPoints.BET)
    public BetVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        BetVo betVo = new BetVo();
        CommonBalanceVo commonBalanceVo = new CommonBalanceVo();
        GameSession gameSession;
        BetDto betDto = new BetDto();
        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            betDto = HttpService.convertJsonToDto(body, BetDto.class);
            // 2. Validate request parameters (Non-database calls)
            this.doValidation(betDto);

            String removedPrefix = vendorService.getExtractToken(betDto.getExternalToken());
            // 3. Verify session token
            gameSession = gameSessionService.verifyToken(removedPrefix);

            if (!(betDto.getGameCodeName()).equals(gameSession.getVendorGameCode())) {
                vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betDto.getGameCodeName(), gameSession);
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            // 5. Process Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto,
                    httpRequestLog.getRequestBody(), httpRequestLog);

            // 6. Set response data
            betVo.setExternalTransactionCode(betDto.getExternalTransactionId());
            betVo.setExternalTransactionDate(VendorService.convertBetOrSettleTime(betDto.getVendorBetTime()));
            commonBalanceVo.setReal(betEvent.getLastBalance().setScale(2, RoundingMode.DOWN));
            commonBalanceVo.setTimestamp(VendorService.returnTime());
            betVo.setBalance(commonBalanceVo);

        } catch (InvalidPlayerException | GameNotSupportedException e) {
            betVo.setError(ErrorVo.from(ResponseCodes.ERR_PLAYER_NOT_FOUND));
            httpService.logError(httpRequestLog, e);
        } catch (TransactionStillProcessingException e) {
            betVo.setError(ErrorVo.from(ResponseCodes.ERR_TRANSACTION_DECLINED));
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            betVo.setExternalTransactionCode(betDto.getExternalTransactionId());
            betVo.setExternalTransactionDate(VendorService.convertBetOrSettleTime(betDto.getVendorBetTime()));
            commonBalanceVo.setReal(e.getBalance().setScale(2, RoundingMode.DOWN));
            commonBalanceVo.setTimestamp(VendorService.returnTime());
            betVo.setBalance(commonBalanceVo);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            betVo.setError(ErrorVo.from(ResponseCodes.ERR_AUTHENTICATION_FAILED));
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            betVo.setError(ErrorVo.from(ResponseCodes.ERR_REGULATORY_GENERAL));
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            betVo.setError(ErrorVo.from(ResponseCodes.ERR_INSUFFICIENT_FUNDS));
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            betVo.setError(ErrorVo.from(ResponseCodes.INTERNAL_ERROR));
            httpService.logError(httpRequestLog, e);
        } finally {
            betVo.setRequestId(betDto.getRequestId());
            httpService.end(httpRequestLog, betVo);
        }
        return betVo;
    }

    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);
    }

    private void doVerification(BetDto betDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            CredentialNotFoundException,
            InvalidPlayerException,
            DisabledGameException {

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        // FindVendorLine
        String kioskPrefix = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.KIOSK_PREFIX);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(kioskPrefix + "_" + gameSession.getVendorPlayerUsername(),
                betDto.getUserName(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}

