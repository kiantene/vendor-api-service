package com.nextgen.gameaggregator.vendor.jili.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
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
public class CancelBetAction {
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

    @PostMapping(path = EndPoints.CANCEL_BET)
    public CancelBetVo CancelBetAction (HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        CancelBetVo cancelBetVo = new CancelBetVo();
        String traceId = httpRequestLog.getTraceId();


        try{
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelBetDto cancelBetDto = HttpService.convertJsonToDto(body, CancelBetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(cancelBetDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(cancelBetDto.getToken());

            // 3. get Bet History for checking
            // TODO : (need change to get by betId)
//            BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(String.valueOf(cancelBetDto.getRound()), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());


            this.doVerification(cancelBetDto, gameSession);

            // 4. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            cancelBetVo.setUsername(gameSession.getVendorPlayerUsername());
            cancelBetVo.setCurrency(gameSession.getVendorCurrencyCode());
            cancelBetVo.setBalance(balance);
//            cancelBetVo.setToken(gameSession.getToken());


        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException invalidRequest) {
            cancelBetVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (AuthenticationException invalidSessionToken) {
            cancelBetVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException e) {
            cancelBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

        } catch (Exception exception) {
            cancelBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally{
            httpService.end(httpRequestLog, cancelBetVo);
        }
        return cancelBetVo;
    }

    private void doValidation(CancelBetDto cancelBetDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(cancelBetDto);
    }
    private void doVerification(CancelBetDto cancelBetDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), cancelBetDto.getToken(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(cancelBetDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), cancelBetDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
