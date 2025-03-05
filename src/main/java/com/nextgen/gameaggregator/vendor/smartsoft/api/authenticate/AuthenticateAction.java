package com.nextgen.gameaggregator.vendor.smartsoft.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticateAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

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

            // 5. Set response data

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
