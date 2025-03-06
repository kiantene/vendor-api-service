package com.nextgen.gameaggregator.vendor.smartsoft.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticateAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;

    public AuthenticateAction(HttpService httpService, GameSessionService gameSessionService, VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(path = EndPoints.SESSION)
    public AuthenticateVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        AuthenticateVo responseVo = new AuthenticateVo();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            String portalName = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_NAME);
            // 5. Set response data
            responseVo.setSessionId(gameSession.getTraceId());
            responseVo.setUserName(gameSession.getVendorPlayerUsername());
            responseVo.setClientExternalKey(gameSession.getToken());
            responseVo.setCurrencyCode(gameSession.getVendorCurrencyCode());
            responseVo.setPortalName(portalName);

        } catch (Exception exception) { // any other exception encountered
            httpService.logError(httpRequestLog, exception);
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(AuthenticateDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AuthenticateDto dto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, CredentialNotFoundException {
        // Verify received vendor player username is the same from game session
//        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), AuthenticationException::new);
//        // Verify vendor line is active
//        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
//        // Verify agent player is active
//        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
//        // Verify vendor game is active
//        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
//        // Verify operator ID
//        ValidationUtils.isEquals(vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator"), dto.getOperatorId(), CredentialNotFoundException::new);
    }

}
