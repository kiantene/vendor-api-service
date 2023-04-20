package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {
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
    @Autowired
    private BetHistoryService betHistoryService;
    @PostMapping(path = EndPoints.BET)
    public BetVo betRequest (HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo betVo = new BetVo();
        String traceId = httpRequestLog.getTraceId();

        try{
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BetDto betDto = HttpService.convertJsonToDto(body, BetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(betDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betDto.getToken());

            this.doVerification(betDto, gameSession);

            // 3. Process bet data
            // 4. Process win data
            ResultType resultType = betDto.getWinloseAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE;
            SettledBetEvent settledBetEvent = walletService.processBetResultPlus(traceId, gameSession, betDto, resultType, body);

            betVo.setUsername(gameSession.getVendorPlayerUsername());
            betVo.setCurrency(gameSession.getVendorCurrencyCode());
            betVo.setBalance(settledBetEvent.getLastBalance());
            betVo.setToken(gameSession.getToken());

        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException invalidRequest) {
            betVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (AuthenticationException invalidSessionToken) {
            betVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            betVo.setResponseCode(ResponseCode.NOT_ENOUGH_BALANCE);

        } catch (DisabledVendorLineException |
                  DisabledGameException |
                  DisabledAgentPlayerException |
                  BetNotFoundException |
                  InvalidOperatorResponseException |
                  InvalidAgentApiCredentialException e) {
            betVo.setResponseCode(ResponseCode.OTHER_ERROR);

        } catch (Exception exception) {
            betVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally{
            httpService.end(httpRequestLog, betVo);
        }
        return betVo;
    }
    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);
    }
    private void doVerification(BetDto betDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), betDto.getToken(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(betDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
