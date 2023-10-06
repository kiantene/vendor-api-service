package com.nextgen.gameaggregator.vendor.jili.api.authenticate;

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
public class AuthAction {
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

    @PostMapping(path = EndPoints.AUTH)
    public AuthVo AuthAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        AuthVo authVo = new AuthVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            AuthDto dto = HttpService.convertJsonToDto(body, AuthDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            this.doVerification(gameSession);

            // 3. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            authVo.setUsername(gameSession.getVendorPlayerUsername());
            authVo.setCurrency(gameSession.getVendorCurrencyCode());
            authVo.setBalance(balance);
            authVo.setToken(gameSession.getToken());

        } catch (DisabledGameException |
                 DisabledAgentPlayerException |
                 InvalidAgentApiCredentialException |
                 InvalidRequestException |
                 DisabledVendorLineException |
                 JsonProcessingException |
                 InvalidOperatorResponseException otherErrorException) {
            authVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, otherErrorException);

        } catch (AuthenticationException invalidSessionToken) {
            authVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, invalidSessionToken);

        } catch (Exception exception) {
            authVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, authVo);
        }

        return authVo;
    }

    private void doValidation(AuthDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }

}
