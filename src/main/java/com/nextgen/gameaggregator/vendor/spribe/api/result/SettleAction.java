package com.nextgen.gameaggregator.vendor.spribe.api.result;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.constant.FreeBetAction;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import com.nextgen.gameaggregator.vendor.spribe.vo.DataVo;
import com.nextgen.gameaggregator.vendor.spribe.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class SettleAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private SettledBetService settledBetService;

    @PostMapping(path = Endpoints.DEPOSIT)
    public ResponseVo settle(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();
        DataVo data = new DataVo();
        String userId = null;
        String currency = null;
        String provider = null;
        String providerTxId = null;
        BigDecimal oldBalance = null;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            SettleDto dto = HttpService.convertJsonToDto(body, SettleDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getSession_token());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGame(), gameSession);

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            userId = gameSession.getVendorPlayerUsername();
            currency = gameSession.getVendorCurrencyCode();
            provider = dto.getProvider();
            providerTxId = dto.getProvider_tx_id();

            // 5. Reject the request if there's no place bet and it's not a free bet
            this.checkRoundExists(dto, gameSession);

            // 6. Retrieve the latest wallet balance from Operator
            oldBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 7. Send bet request to Operator
            ResultType resultType = getResultType(dto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // 8. Set response data
            data.setOperator_tx_id(traceId);
            data.setNew_balance(AmountConverter.convertBalanceToUnit(balance));
            data.setOld_balance(AmountConverter.convertBalanceToUnit(oldBalance));
            data.setUser_id(userId);
            data.setCurrency(currency);
            data.setProvider(provider);
            data.setProvider_tx_id(providerTxId);
            vo.setErrorCode(ErrorCodes.SUCCESS);
            vo.setData(data);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorCode(ErrorCodes.INVALID_TOKEN);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (BetNotFoundException betNotFoundException) {
            vo.setErrorCode(ErrorCodes.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            data.setOperator_tx_id(traceId);
            data.setNew_balance(betResultIdempotentViolationException.getBalance());
            data.setOld_balance(betResultIdempotentViolationException.getBalance());
            data.setUser_id(userId);
            data.setCurrency(currency);
            data.setProvider(provider);
            data.setProvider_tx_id(betResultIdempotentViolationException.getBetId());
            vo.setErrorCode(ErrorCodes.DUPLICATE_TRANSACTION);
            vo.setData(data);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_DUPLICATE_REQUEST.code) ||
                    invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_TRANSACTION_DUPLICATED.code)) {
                data.setOperator_tx_id(traceId);
                data.setNew_balance(oldBalance);
                data.setOld_balance(oldBalance);
                data.setUser_id(userId);
                data.setCurrency(currency);
                data.setProvider(provider);
                data.setProvider_tx_id(providerTxId);
                vo.setErrorCode(ErrorCodes.DUPLICATE_TRANSACTION);
                vo.setData(data);

            } else if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                vo.setErrorCode(ErrorCodes.INSUFFICIENT_FUND);

            } else {
                vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidAgentApiCredentialException | VendorCurrencyNotSupportException | InvalidRequestException |
                 DisabledVendorLineException | DisabledAgentPlayerException |
                 DisabledGameException | MergedBetDataIntegrityException | InsufficientBalanceException |
                 TransactionStillProcessingException | GameNotSupportedException |
                 CurrencyNotSupportedException internalErrorExeption) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, internalErrorExeption);

        } catch (Exception exception) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, SettleDto dto, GameSession gameSession) throws InvalidPlayerException, DisabledAgentPlayerException, DisabledVendorLineException,
            DisabledGameException, AuthenticationException, GameNotSupportedException, CurrencyNotSupportedException {

        // Check game session status (0 = inactive)
        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUser_id(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUser_id());
    }

    private ResultType getResultType(SettleDto dto) {
        ResultType resultType = ResultType.BET_LOSE;
        BigDecimal zero = BigDecimal.ZERO;

        if (dto.getWinAmount().compareTo(zero) > 0) {
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }

    private void checkRoundExists(SettleDto dto, GameSession gameSession) throws BetNotFoundException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), dto.getRoundId());

        if (settledBetList.isEmpty() && !FreeBetAction.list.contains(dto.getAction())) throw new BetNotFoundException();
    }
}
