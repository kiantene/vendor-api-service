package com.nextgen.gameaggregator.vendor.winfinity.api.bet;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;

@Service
public class PayinService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SettledBetService settledBetService;

    public ResponseVo payin(String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        try {
            // Convert original request body into dto
            PayinDto dto = HttpService.convertJsonToDto(body, PayinDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession with token
            GameSession gameSession = gameSessionService.verifyToken(dto.getMsid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // Check if the round exists
            List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), dto.getRoundId());

            if (settledBetList.isEmpty()) {
                BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
                vo.setDataVo(traceId, betEvent.getLastBalance());

            } else {
                vo.setErrorVo(ErrorCodes.TRANS_ALREADY_EXISTS);
            }

        } catch (JsonProcessingException | TransactionStillProcessingException | InvalidRequestException badRequestException) {
            httpService.logError(httpRequestLog, badRequestException);
            vo.setErrorVo(ErrorCodes.BAD_REQUEST);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorVo(ErrorCodes.WRONG_SESSION);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            vo.setErrorVo(ErrorCodes.NOT_ENOUGH_FUND);

        } catch (InvalidOperatorResponseException | InvalidAgentApiCredentialException | DisabledVendorLineException
                | DisabledAgentPlayerException unknownErrorException) {
            httpService.logError(httpRequestLog, unknownErrorException);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            vo.setErrorVo(ErrorCodes.TRANS_ALREADY_EXISTS);

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            vo.setErrorVo(ErrorCodes.GAME_NOT_AVAILABLE);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            httpService.logError(httpRequestLog, currencyNotSupportedException);
            vo.setErrorVo(ErrorCodes.CURRENCY_NOT_ALLOWED);

        } catch (InvalidPlayerException invalidPlayerException) {
            httpService.logError(httpRequestLog, invalidPlayerException);
            vo.setErrorVo(ErrorCodes.PLAYER_NOT_ALLOWED);

        } catch (Exception exception) { // Any other exception encountered
            httpService.logError(httpRequestLog, exception);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
        }

        return vo;
    }

    private void doValidation(PayinDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(PayinDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException {
        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUid());
    }
}
