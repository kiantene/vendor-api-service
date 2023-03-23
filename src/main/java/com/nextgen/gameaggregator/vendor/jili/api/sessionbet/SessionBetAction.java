package com.nextgen.gameaggregator.vendor.jili.api.sessionbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.api.bet.WinDto;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.Formats;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

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

            if (sessionBetDto.getType() == Formats.SESSION_BET_TYPE_BET) {
                // Process bet data
                BetEvent betEvent = walletService.processBet(traceId, gameSession, sessionBetDto, body);

                sessionBetVo.setUsername(gameSession.getVendorPlayerUsername());
                sessionBetVo.setCurrency(gameSession.getCurrencyCode());
                sessionBetVo.setBalance(betEvent.getLastBalance());
                sessionBetVo.setToken(gameSession.getToken());
            } else if (sessionBetDto.getType() == Formats.SESSION_BET_TYPE_SETTLE) {
                // Process win data
                WinDto winDto = new ObjectMapper().convertValue(sessionBetDto, WinDto.class);
                winDto.setExternalTransactionId(sessionBetDto.getReqId());
                winDto.setAmount(sessionBetDto.getWinloseAmount());
                winDto.setWinType(getWinType(sessionBetDto));
                winDto.setEffectiveTurnover(sessionBetDto.getTurnover());
                BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);

                // Emit event for additional asynchronous processing
                EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));

                sessionBetVo.setUsername(gameSession.getVendorPlayerUsername());
                sessionBetVo.setCurrency(gameSession.getCurrencyCode());
                sessionBetVo.setBalance(betResultEvent.getLastBalance());
                sessionBetVo.setToken(gameSession.getToken());
            } else {
                throw new InvalidRequestException();
            }

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
                 BetResultNotFoundException |
                 DuplicateExternalTransactionIdException |
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

    private WinType getWinType(SessionBetDto sessionBetDto) {
        return (sessionBetDto.getWinloseAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
    }
}
