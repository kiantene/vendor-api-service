package com.nextgen.gameaggregator.vendor.jili.api.sessionbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.eventing.events.UnsettledBetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.Formats;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SessionBetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @PostMapping(path = EndPoints.SESSION_BET)
    public SessionBetVo SessionBetAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        SessionBetVo sessionBetVo = new SessionBetVo();
        String traceId = httpRequestLog.getTraceId();

        try{
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
                    UnsettledBetEvent unsettledBetEvent = walletService.processBet(traceId, gameSession, sessionBetDto, body);
                    sessionBetVo.setBalance(unsettledBetEvent.getLastBalance());
                }
                case Formats.SESSION_BET_TYPE_SETTLE -> {
                    SettledBetEvent settledBetEvent = walletService.processSettledBet(traceId, gameSession, sessionBetDto);
                    sessionBetVo.setBalance(settledBetEvent.getLastBalance());
                }
                default -> throw new InvalidRequestException();
            }

            sessionBetVo.setUsername(gameSession.getVendorPlayerUsername());
            sessionBetVo.setCurrency(gameSession.getVendorCurrencyCode());
            sessionBetVo.setToken(gameSession.getToken());

        } catch(InvalidRequestException |
               JsonProcessingException |
               GameNotSupportedException |
               CurrencyNotSupportedException invalidRequest){
            sessionBetVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (AuthenticationException invalidSessionToken) {
            sessionBetVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            sessionBetVo.setResponseCode(ResponseCode.NOT_ENOUGH_BALANCE);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 BetNotFoundException |
                 InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException e) {
            sessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

        } catch (Exception exception) {
            sessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        }finally {
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
            CurrencyNotSupportedException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), sessionBetDto.getToken(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(sessionBetDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), sessionBetDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
