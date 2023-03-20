package com.nextgen.gameaggregator.vendor.spadegaming.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.dto.BalanceDto;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AcctInfoVo;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AuthBalanceVo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;

@Service
public class BalanceService {

    @Autowired
    private HttpService httpService;

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
            // Verify the user token and get the corresponding game session
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());
            // Get the user's account balance using the game session and trace ID
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Populate the AcctInfoVo object with user details
            acctInfoVo.setAcctId(gameSession.getVendorPlayerUsername());
            acctInfoVo.setBalance(balance);
            acctInfoVo.setUserName(gameSession.getVendorPlayerUsername());
            acctInfoVo.setCurrency(gameSession.getCurrencyCode());
            acctInfoVo.setSiteId(gameSession.getVendorId());
            
            // Populate the AuthBalanceVo object with response details
            authBalanceVo.setAcctInfo(acctInfoVo);
            authBalanceVo.setMerchantCode(Credentials.MERCHANT_CODE);
            authBalanceVo.setResponseCode(ResponseCode.SUCCESS);
            authBalanceVo.setSerialNo(traceId);
    
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
}
