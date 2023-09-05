package com.nextgen.gameaggregator.vendor.spadegaming.api.authenticate;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class AuthenticateService {
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
    
    public AuthBalanceVo authenticate(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Get the request body and trace ID from the logging
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getId();

        AuthBalanceVo authBalanceVo = new AuthBalanceVo();
        AcctInfoVo acctInfoVo = new AcctInfoVo();

        try {
            // Convert the request body to an AuthenticateDto object
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);
            authBalanceVo.setMerchantCode(dto.getMerchantCode());
            authBalanceVo.setSerialNo(traceId);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Get game session with player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());
            String merchantCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.MERCHANT_CODE);
            this.doVerification(dto, gameSession, merchantCode);
            // Get the user's account balance using the game session and trace ID
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

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
        } catch (AuthenticationException authenticationException) {
            // token validation failed 
            authBalanceVo.setResponseCode(ResponseCode.TOKEN_VALIDATION_FAILED);
        } catch (CredentialNotFoundException | UnableToFindCredentialsException |
                 DisabledVendorLineException | DisabledAgentPlayerException |
                 DisabledGameException | InvalidAgentApiCredentialException e) {
            // merchant not found or service inaccessible 
            authBalanceVo.setResponseCode(ResponseCode.MERCHANT_NOT_FOUND);
        } catch (InvalidRequestException | InvalidOperatorResponseException e) {
            // invalid request 
            authBalanceVo.setResponseCode(ResponseCode.INVALID_REQUEST);
        } catch (JsonProcessingException | InvalidFormatException e) {
            // invalid format 
            authBalanceVo.setResponseCode(ResponseCode.INVALID_FORMAT);
        } catch (InvalidPlayerException e) {
            // account not found 
            authBalanceVo.setResponseCode(ResponseCode.ACCT_NOT_FOUND);
        } catch (IllegalArgumentException e) {  
            // invalid parameter 
            authBalanceVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
        } catch (Exception e) {
            // others
            authBalanceVo.setResponseCode(ResponseCode.SYSTEM_ERROR);
        }
        finally {
           // End the HTTP request logging and return the AuthBalanceVo object
            httpService.end(httpRequestLog, authBalanceVo);

        }

        return authBalanceVo;
    }

    private void doValidation(AuthenticateDto dto) throws InvalidRequestException, InvalidFormatException{
        // Validate token
        ValidationUtils.validateLength(dto.getToken(), 1, 80, InvalidFormatException::new);
        Matcher m = Pattern.compile(ValidationUtils.ALPHANUMERIC_DASH_REGEX).matcher(dto.getToken());
        if(!m.matches()) throw new InvalidFormatException();

        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AuthenticateDto dto, GameSession gameSession, String merchantCode)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, 
            DisabledGameException, UnableToFindCredentialsException, InvalidPlayerException, CredentialNotFoundException{

        // Verify received merchant code is same from Credentials merchant code 
        ValidationUtils.isEquals(merchantCode, dto.getMerchantCode(), UnableToFindCredentialsException::new);

        // Verify received token is the same from game session
        ValidationUtils.isEquals(gameSession.getToken(), dto.getToken(), AuthenticationException::new);
        
        // Verify received acct id is the same from vendor player username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAcctId(), InvalidPlayerException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());


    }
}
