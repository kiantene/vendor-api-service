package com.nextgen.gameaggregator.vendor.jili.api.cancelsessionbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.RawGameSession;
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
public class CancelSessionBetAction {
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
    @PostMapping(path = EndPoints.CANCEL_SESSION_BET)
    public CancelSessionBetVo CancelSessionBetAction (HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        CancelSessionBetVo cancelSessionBetVo = new CancelSessionBetVo();
        String traceId = httpRequestLog.getTraceId();


        try{
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelSessionBetDto cancelSessionBetDto = HttpService.convertJsonToDto(body, CancelSessionBetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(cancelSessionBetDto);

            // 2. Verify session token
            RawGameSession rawGameSession = gameSessionService.verifyToken(cancelSessionBetDto.getToken());

            // 3. get Bet History for checking
            // TODO : (need change to get by betId)
//            BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(String.valueOf(cancelSessionBetDto.getRound()), rawGameSession.getVendorGameId(), rawGameSession.getVendorPlayerId());


            this.doVerification(cancelSessionBetDto, rawGameSession);

            // 4. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, rawGameSession);

            cancelSessionBetVo.setUsername(rawGameSession.getVendorPlayerUsername());
            cancelSessionBetVo.setCurrency(rawGameSession.getVendorCurrencyCode());
            cancelSessionBetVo.setBalance(balance);
//            cancelSessionBetVo.setToken(rawGameSession.getToken());


        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException invalidRequest) {
            cancelSessionBetVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (AuthenticationException invalidSessionToken) {
            cancelSessionBetVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 DisabledAgentPlayerException |
                 InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException e) {
            cancelSessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);

        } catch (Exception exception) {
            cancelSessionBetVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally{
            httpService.end(httpRequestLog, cancelSessionBetVo);
        }
        return cancelSessionBetVo;
    }

    private void doValidation(CancelSessionBetDto cancelSessionBetDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(cancelSessionBetDto);
    }
    private void doVerification(CancelSessionBetDto cancelSessionBetDto, RawGameSession rawGameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(rawGameSession.getToken(), cancelSessionBetDto.getToken(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(rawGameSession.getVendorGameCode(), String.valueOf(cancelSessionBetDto.getGame()), GameNotSupportedException::new);
        ValidationUtils.isEquals(rawGameSession.getVendorCurrencyCode(), cancelSessionBetDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(rawGameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(rawGameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(rawGameSession.getVendorGameId());

    }
}
