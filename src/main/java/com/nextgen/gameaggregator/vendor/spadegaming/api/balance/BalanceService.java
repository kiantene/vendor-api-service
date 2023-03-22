package com.nextgen.gameaggregator.vendor.spadegaming.api.balance;

import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AcctInfoVo;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AuthBalanceVo;

@Service
public class BalanceService {

    @Autowired
    private HttpService httpService;

    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private AgentPlayerService agentPlayerService;

    @Autowired
    private VendorGameService vendorGameService;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private WalletService walletService;
    
    public AuthBalanceVo balance(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);
        
        // Get the request body and trace ID from the logging
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getTraceId();

        // Create new AuthBalanceVo and AcctInfoVo objects
        AuthBalanceVo authBalanceVo = new AuthBalanceVo();
        AcctInfoVo acctInfoVo = new AcctInfoVo();
        authBalanceVo.setMerchantCode(Credentials.MERCHANT_CODE);
        authBalanceVo.setSerialNo(traceId);

        try {
            // Convert the request body to an BalanceDto object
            BalanceDto dto = HttpService.convertJsonToDto(body, BalanceDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Verify the user token and get the corresponding game session
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());
            this.doVerification(dto, gameSession);
            // Get the user's account balance using the game session and trace ID
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Populate the AcctInfoVo object with user details
            acctInfoVo.setAcctId(gameSession.getVendorPlayerUsername());
            acctInfoVo.setBalance(balance);
            acctInfoVo.setUserName(gameSession.getVendorPlayerUsername());
            acctInfoVo.setCurrency(gameSession.getCurrencyCode());
            acctInfoVo.setSiteId(gameSession.getAgentId());
            
            // Populate the AuthBalanceVo object with response details
            authBalanceVo.setAcctInfo(acctInfoVo);
            authBalanceVo.setMerchantCode(Credentials.MERCHANT_CODE);
            authBalanceVo.setResponseCode(ResponseCode.SUCCESS);
            authBalanceVo.setSerialNo(traceId);

        } catch (InvalidRequestException invalidRequestException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            authBalanceVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            authBalanceVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (DisabledGameException disabledGameException) {
            authBalanceVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);
    
        } catch (JsonProcessingException jsonProcessingException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_FORMAT);

        } catch (AuthenticationException authenticationException) {
            authBalanceVo.setResponseCode(ResponseCode.TOKEN_VALIDATION_FAILED);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (IllegalArgumentException illegalArgumentException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
            
        } finally {
            // End the HTTP request logging and return the AuthBalanceVo object
            httpService.end(httpRequestLog, authBalanceVo);
             
         }

        return authBalanceVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException{
        
        // Verify received vendor player username is the same from game session
        // Comparison for game session value will always be using AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAcctId(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
