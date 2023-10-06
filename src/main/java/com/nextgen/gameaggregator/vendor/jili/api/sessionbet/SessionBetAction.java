package com.nextgen.gameaggregator.vendor.jili.api.sessionbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.Formats;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jili.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SessionBetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.SESSION_BET)
    public SessionBetVo SessionBetAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        SessionBetVo sessionBetVo = new SessionBetVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            SessionBetDto sessionBetDto = HttpService.convertJsonToDto(body, SessionBetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(sessionBetDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(sessionBetDto.getToken());

            this.doVerification(sessionBetDto, gameSession);

            switch (sessionBetDto.getType()) {
                case Formats.SESSION_BET_TYPE_BET -> {
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, sessionBetDto, body, httpRequestLog);
                    sessionBetVo.setBalance(betEvent.getLastBalance());
                }
                case Formats.SESSION_BET_TYPE_SETTLE -> {
                    // Check if bet already settled
                    this.verifySettledBet(gameSession, sessionBetDto);

                    // Get result type
                    ResultType resultType = this.getResultType(sessionBetDto);

                    // Verify unsettle bet
                    this.verifyUnsettleBet(sessionBetDto, gameSession);

                    // Process bet
                    BigDecimal balance = walletService.processBetResult(traceId, gameSession, sessionBetDto, resultType, vendorService, httpRequestLog);
                    sessionBetVo.setBalance(balance);
                }
                default -> throw new InvalidRequestException();
            }

            sessionBetVo.setUsername(gameSession.getVendorPlayerUsername());
            sessionBetVo.setCurrency(gameSession.getVendorCurrencyCode());
            sessionBetVo.setToken(gameSession.getToken());

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            sessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            sessionBetVo.setResponseCode(ResponseCode.ALREADY_ACCEPTED);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException invalidRequest) {
            sessionBetVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, invalidRequest);

        } catch (AuthenticationException invalidSessionToken) {
            sessionBetVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, invalidSessionToken);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            sessionBetVo.setResponseCode(ResponseCode.NOT_ENOUGH_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //SC_INSUFFICIENT_FUNDS
            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                sessionBetVo.setResponseCode(ResponseCode.NOT_ENOUGH_BALANCE);
            } else {
                sessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (BetNotFoundException betNotFoundException) {
            sessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException otherErrorException) {
            sessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, otherErrorException);

        } catch (Exception exception) {
            sessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, sessionBetVo);
        }
        return sessionBetVo;
    }

    private void doValidation(SessionBetDto sessionBetDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(sessionBetDto);
    }

    private void doVerification(SessionBetDto sessionBetDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidPlayerException {

        if(sessionBetDto.getType().equals(Formats.SESSION_BET_TYPE_BET)) {
            // validate vendor username, agent vendor line, player status, and game status
            validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        }

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(sessionBetDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), sessionBetDto.getCurrency(), CurrencyNotSupportedException::new);

    }

    private ResultType getResultType(SessionBetDto dto) {
        // if transaction amount has more than 0 means WIN else LOSE
        ResultType resultType = (dto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.END;

        return resultType;
    }

    private void verifyUnsettleBet(SessionBetDto dto, GameSession gameSession) throws BetNotFoundException {
        List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

        if (unsettledBetList.isEmpty()) {
            throw new BetNotFoundException("Cannot find round Id: " + dto.getRoundId());
        }
    }

    private void verifySettledBet(GameSession gameSession, SessionBetDto dto) throws BetResultIdempotentViolationException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), String.valueOf(dto.getSessionId()));

        if (settledBetList.size() > 0) {
            throw new BetResultIdempotentViolationException();
        }
    }
}
