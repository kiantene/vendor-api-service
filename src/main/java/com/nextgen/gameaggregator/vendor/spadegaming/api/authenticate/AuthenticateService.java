package com.nextgen.gameaggregator.vendor.spadegaming.api.authenticate;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nextgen.gameaggregator.entity.RawGameSession;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.UnableToFindCredentialsException;
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
        String traceId = httpRequestLog.getTraceId();

        AuthBalanceVo authBalanceVo = new AuthBalanceVo();
        AcctInfoVo acctInfoVo = new AcctInfoVo();

        try {
            // Convert the request body to an AuthenticateDto object
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);
            authBalanceVo.setMerchantCode(dto.getMerchantCode());
            authBalanceVo.setSerialNo(traceId);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Verify the user token and get the corresponding game session
            RawGameSession rawGameSession = gameSessionService.verifyToken(dto.getToken());
            String merchantCode = vendorLineService.getCredentialValueByName(rawGameSession.getVendorLineId(), Credentials.MERCHANT_CODE);
            this.doVerification(dto, rawGameSession, merchantCode);
            // Get the user's account balance using the game session and trace ID
            BigDecimal balance = walletService.getBalance(traceId, rawGameSession);

            // Populate the AcctInfoVo object with user details
            acctInfoVo.setAcctId(rawGameSession.getVendorPlayerUsername());
            acctInfoVo.setBalance(balance);
            acctInfoVo.setUserName(rawGameSession.getVendorPlayerUsername());
            acctInfoVo.setCurrency(rawGameSession.getVendorCurrencyCode());
            acctInfoVo.setSiteId(rawGameSession.getAgentId());

            // Populate the AuthBalanceVo object with response details
            authBalanceVo.setAcctInfo(acctInfoVo);
            authBalanceVo.setMerchantCode(dto.getMerchantCode());
            authBalanceVo.setResponseCode(ResponseCode.SUCCESS);
            authBalanceVo.setSerialNo(traceId);

        } catch (InvalidRequestException invalidRequestException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (DisabledVendorLineException | 
                DisabledAgentPlayerException | 
                DisabledGameException serviceException) {
            authBalanceVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (JsonProcessingException jsonProcessingException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_FORMAT);

        } catch (AuthenticationException authenticationException) {
            authBalanceVo.setResponseCode(ResponseCode.TOKEN_VALIDATION_FAILED);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            authBalanceVo.setResponseCode(ResponseCode.MERCHANT_NOT_FOUND);

        } catch (IllegalArgumentException illegalArgumentException) {
            authBalanceVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
            authBalanceVo.setResponseCode(ResponseCode.MERCHANT_NOT_FOUND);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            authBalanceVo.setResponseCode(ResponseCode.MERCHANT_NOT_FOUND);
            
        } catch (InvalidPlayerException invalidPlayerException) {
            authBalanceVo.setResponseCode(ResponseCode.ACCT_NOT_FOUND);

        } catch (InvalidFormatException invalidFormatException) {
            authBalanceVo.setResponseCode(ResponseCode.TOKEN_VALIDATION_FAILED);

        } finally {
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

    private void doVerification(AuthenticateDto dto, RawGameSession rawGameSession, String merchantCode)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, 
            DisabledGameException, UnableToFindCredentialsException, InvalidPlayerException, CredentialNotFoundException{

        // Verify received merchant code is same from Credentials merchant code 
        ValidationUtils.isEquals(merchantCode, dto.getMerchantCode(), UnableToFindCredentialsException::new);

        // Verify received token is the same from game session
        ValidationUtils.isEquals(rawGameSession.getToken(), dto.getToken(), AuthenticationException::new);
        
        // Verify received acct id is the same from vendor player username
        ValidationUtils.isEquals(rawGameSession.getVendorPlayerUsername(), dto.getAcctId(), InvalidPlayerException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(rawGameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(rawGameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(rawGameSession.getVendorGameId());


    }
}
