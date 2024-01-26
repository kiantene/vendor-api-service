package com.nextgen.gameaggregator.vendor.hacksaw.api.authenticate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksaw.service.VendorService;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class AuthService {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorService vendorService;

    public ResponseVo authenticate(HttpRequestLog httpRequestLog, String traceId) {

        AuthVo vo = new AuthVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            AuthDto dto = HttpService.convertJsonToDto(body, AuthDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify launch token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Set response data
            vo.setExternalPlayerId(gameSession.getVendorPlayerUsername());
            vo.setAccountCurrency(gameSession.getVendorCurrencyCode());
            vo.setAccountBalance(balance.longValue());
            vo.setExternalSessionId(dto.getToken()); // using back our game token

        } catch (AuthenticationException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_USER_OR_TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, e);

        } catch (DisabledAgentPlayerException e) {
            vo.setResponseCodes(ResponseCodes.ACCOUNT_LOCKED);
            httpService.logError(httpRequestLog, e);

        } catch (JsonProcessingException | InvalidRequestException | CredentialNotFoundException |
                 GameNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_ACTION);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);

        }

        return vo;
    }

    private void doValidation(AuthDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AuthDto dto, GameSession gameSession) throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            InvalidRequestException,
            CredentialNotFoundException {

        // Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), dto.getToken(), AuthenticationException::new);

        // Verify valid game id
        vendorService.verifyVendorGameCode(gameSession, dto.getGameId().toString());

        // Retrieve vendor line operatorToken and secretKey for validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        ValidationUtils.isEquals(dto.getSecret(), secretKey, CredentialNotFoundException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
