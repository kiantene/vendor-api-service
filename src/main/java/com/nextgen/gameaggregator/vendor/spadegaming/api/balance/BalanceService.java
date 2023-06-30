package com.nextgen.gameaggregator.vendor.spadegaming.api.balance;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AcctInfoVo;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.AuthBalanceVo;

import jakarta.servlet.http.HttpServletRequest;

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
        String traceId = httpRequestLog.getId();

        // Create new AuthBalanceVo and AcctInfoVo objects
        AuthBalanceVo authBalanceVo = new AuthBalanceVo();
        AcctInfoVo acctInfoVo = new AcctInfoVo();

        try {
            // Convert the request body to an BalanceDto object
            BalanceDto dto = HttpService.convertJsonToDto(body, BalanceDto.class);
            authBalanceVo.setMerchantCode(dto.getMerchantCode());
            authBalanceVo.setSerialNo(traceId);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Verify the user token and get the corresponding game session
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());
            String merchantCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.MERCHANT_CODE);
            this.doVerification(dto, gameSession, merchantCode);
            // Get the user's account balance using the game session and trace ID
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Populate the AcctInfoVo object with user details
            acctInfoVo.setAcctId(gameSession.getVendorPlayerUsername());
            acctInfoVo.setBalance(balance);
            acctInfoVo.setUserName(gameSession.getVendorPlayerUsername());
            acctInfoVo.setCurrency(gameSession.getVendorCurrencyCode());
            acctInfoVo.setSiteId(gameSession.getAgentId());
            
            // Populate the AuthBalanceVo object with response details
            authBalanceVo.setAcctInfo(acctInfoVo);
            authBalanceVo.setMerchantCode(dto.getMerchantCode());
            authBalanceVo.setResponseCode(ResponseCode.SUCCESS);
            authBalanceVo.setSerialNo(traceId);
        } catch (AuthenticationException e) {
            // handle account not found errors
            authBalanceVo.setResponseCode(ResponseCode.ACCT_NOT_FOUND);
        } catch (CredentialNotFoundException | UnableToFindCredentialsException e) {
            // handle merchant not found errors
            authBalanceVo.setResponseCode(ResponseCode.MERCHANT_NOT_FOUND);
        } catch (DisabledVendorLineException | DisabledAgentPlayerException |
                DisabledGameException e) {
            // handle service inaccessible errors
            authBalanceVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);
        } catch (InvalidRequestException | InvalidOperatorResponseException |
                InvalidAgentApiCredentialException | GameNotSupportedException e) {
            // handle invalid request errors
            authBalanceVo.setResponseCode(ResponseCode.INVALID_REQUEST);
        } catch (JsonProcessingException e) {
            // handle invalid format errors
            authBalanceVo.setResponseCode(ResponseCode.INVALID_FORMAT);
        } catch (IllegalArgumentException e) {
            // handle invalid parameter errors
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

    private void doVerification(BalanceDto dto, GameSession gameSession, String merchantCode)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, 
            DisabledGameException, UnableToFindCredentialsException, GameNotSupportedException{

        // Verify received merchant code is same from Credentials merchant code 
        ValidationUtils.isEquals(merchantCode, dto.getMerchantCode(), UnableToFindCredentialsException::new);
        
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAcctId(), AuthenticationException::new);
        
        // Verify received game code is the same from vendor game code
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameCode() != null ? dto.getGameCode() : gameSession.getVendorGameCode(), GameNotSupportedException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
