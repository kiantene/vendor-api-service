package com.nextgen.gameaggregator.vendor.ezugi.api.authentication;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.service.VendorService;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticationAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @PostMapping(path = EndPoints.AUTHENTICATION)
    public CommonVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        AuthenticationVo authenticationVo = new AuthenticationVo();
        try {
            String body = httpRequestLog.getRequestBody();
            AuthenticationDto authenticationDto = HttpService.convertJsonToDto(body, AuthenticationDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(authenticationDto);

            // Remove last 2 char LT to get our session token, because the session and launch token cannot be same
            String token = authenticationDto.getToken().substring(0, authenticationDto.getToken().length() - 2);

            // Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(token);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(token, gameSession, httpRequestLog, request, authenticationDto);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            authenticationVo.setToken(gameSession.getToken());
            authenticationVo.setOperatorId(authenticationDto.getOperatorId());
            authenticationVo.setUid(gameSession.getVendorPlayerUsername());
            authenticationVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            authenticationVo.setCurrency(gameSession.getVendorCurrencyCode());
            authenticationVo.setErrorCode(ResponseCodes.OK);
            authenticationVo.setTimestamp(System.currentTimeMillis());
        } catch (AuthenticationException e) {
            authenticationVo.setErrorCode(ResponseCodes.TOKEN_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            authenticationVo.setErrorCode(ResponseCodes.USER_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledAgentPlayerException e) {
            authenticationVo.setErrorCode(ResponseCodes.USER_BLOCKED);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            authenticationVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            authenticationVo.setErrorDescription("Invalid Hash");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException | IOException e) {
            authenticationVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            authenticationVo.setErrorDescription("Invalid parameter");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException | NoSuchAlgorithmException |
                 InvalidKeyException | DisabledVendorLineException | CredentialNotFoundException |
                 InvalidAgentApiCredentialException | DisabledGameException e) {
            authenticationVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            authenticationVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            if (authenticationVo.getErrorDescription() == null) {
                authenticationVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(authenticationVo.getErrorCode()));
            }
            httpService.end(httpRequestLog, authenticationVo);
        }
        return authenticationVo;
    }

    private void doValidation(AuthenticationDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(String token, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request, AuthenticationDto authenticationDto)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, NoSuchAlgorithmException, InvalidKeyException, CredentialNotFoundException, InvalidPlayerException, IOException, InvalidSignatureException, InvalidRequestException {
        // Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), token, AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify Operator Id from vendor given
        String operatorId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_ID);
        ValidationUtils.isEquals(operatorId, String.valueOf(authenticationDto.getOperatorId()), InvalidRequestException::new);
        
        // Verify Signature key from vendor given
        String hashKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.HASH_KEY);
        VendorService.verifyHash(hashKey, httpRequestLog.getRequestBody(), request.getHeader("hash"));
    }
}
