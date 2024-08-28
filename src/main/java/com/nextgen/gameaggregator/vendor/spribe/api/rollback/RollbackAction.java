package com.nextgen.gameaggregator.vendor.spribe.api.rollback;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.service.VendorService;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import com.nextgen.gameaggregator.vendor.spribe.vo.DataVo;
import com.nextgen.gameaggregator.vendor.spribe.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class RollbackAction {

    private final HttpService httpService;
    private final SettledBetService settledBetService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;

    @Autowired
    public RollbackAction(HttpService httpService,
                          SettledBetService settledBetService,
                          GameSessionService gameSessionService,
                          WalletService walletService,
                          VendorLineService vendorLineService,
                          AgentPlayerService agentPlayerService,
                          VendorGameService vendorGameService,
                          VendorService vendorService) {

        this.httpService = httpService;
        this.settledBetService = settledBetService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = Endpoints.ROLLBACK)
    public ResponseVo rollback(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();
        DataVo data = new DataVo();
        String userId = null;
        String currency = null;
        String provider = null;
        String providerTxId = null;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            RollbackDto dto = HttpService.convertJsonToDto(body, RollbackDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession;
            String newToken = (dto.getSession_token() != null) ? dto.getSession_token() : traceId;

            try {
                gameSession = gameSessionService.verifyToken(dto.getSession_token());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGame(), gameSession);
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getUser_id());
                gameSessionService.updateByVendorGameCode(gameSession, dto.getGame());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(newToken);
                gameSession.setVendorToken(newToken);
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            userId = gameSession.getVendorPlayerUsername();
            currency = gameSession.getVendorCurrencyCode();
            provider = dto.getProvider();
            providerTxId = dto.getProvider_tx_id();

            // 5. Check whether if the request is a place bet not result
//            SettledBet rawSettledBet = this.checkSettledBetRequest(dto, gameSession);
//            BigDecimal winAmount = rawSettledBet.getWinAmount();
//            Integer freeSpin = rawSettledBet.getIsFreespin();

            // 6. Zero win amount & no free spin considered a valid rollback scenario (Only place bet can rollback)
//            this.checkValidRollback(winAmount, freeSpin);

            // 7. Send rollback request to Operator
            WalletRequest walletRequest = walletService.processRollback(dto, gameSession, vendorService, httpRequestLog);

            // 8. Set response data
            data.setOperator_tx_id(traceId);
            data.setNew_balance(AmountConverter.convertBalanceToUnit(walletRequest.getBalanceAfter()));
            data.setOld_balance(AmountConverter.convertBalanceToUnit(walletRequest.getBalanceBefore()));
            data.setUser_id(userId);
            data.setCurrency(currency);
            data.setProvider(provider);
            data.setProvider_tx_id(providerTxId);
            vo.setErrorCode(ErrorCodes.SUCCESS);
            vo.setData(data);

        } catch (RecordNotFoundException | BetNotFoundException transactionNotFoundException) {
            vo.setErrorCode(ErrorCodes.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, transactionNotFoundException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_DUPLICATE_REQUEST.code) ||
                    invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_TRANSACTION_DUPLICATED.code)) {
                vo.setErrorCode(ErrorCodes.DUPLICATE_TRANSACTION);

            } else if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                vo.setErrorCode(ErrorCodes.INSUFFICIENT_FUND);

            } else {
                vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorCode(ErrorCodes.INVALID_TOKEN);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidRequestException | DisabledVendorLineException | DisabledAgentPlayerException |
                 DisabledGameException | InvalidAgentApiCredentialException |
                 TransactionStillProcessingException | VendorCurrencyNotSupportException |
                 GameNotSupportedException internalErrorException) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, internalErrorException);

        } catch (BetResultIdempotentViolationException |
                 BetRefundIdempotentViolationException idempotentViolationException) {
            data.setOperator_tx_id(traceId);
            data.setNew_balance(AmountConverter.convertBalanceToUnit(BigDecimal.ZERO));
            data.setOld_balance(AmountConverter.convertBalanceToUnit(BigDecimal.ZERO));
            data.setUser_id(userId);
            data.setCurrency(currency);
            data.setProvider(provider);
            data.setProvider_tx_id(providerTxId);
            vo.setErrorCode(ErrorCodes.DUPLICATE_TRANSACTION);
            vo.setData(data);
            httpService.logError(httpRequestLog, idempotentViolationException);

        } catch (Exception exception) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException,
            DisabledGameException, GameNotSupportedException {

        // Check game session status (0 = inactive)
        //if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUser_id(), AuthenticationException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGame()), GameNotSupportedException::new);

        // Verify vendor line is active
        //vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        //agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        //vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

    private SettledBet checkSettledBetRequest(RollbackDto dto, GameSession gameSession) throws BetNotFoundException {
        // Check whether if the request is a place bet not result
        SettledBet rawSettledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(dto.getRollback_provider_tx_id(), dto.getAction_id(), gameSession.getVendorId(),
                gameSession.getVendorPlayerId());
        return rawSettledBet;
    }

    private void checkValidRollback(BigDecimal winAmount, Integer freeSpin) throws InvalidRequestException {
        // Zero win amount & no free spin considered a valid rollback scenario (Only place bet can rollback)
        if (!(winAmount.equals(BigDecimal.ZERO) && freeSpin == 0)) throw new InvalidRequestException();
    }
}
