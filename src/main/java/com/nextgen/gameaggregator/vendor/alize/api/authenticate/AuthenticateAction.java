package com.nextgen.gameaggregator.vendor.alize.api.authenticate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.alize.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alize.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.alize.vo.CommonVo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = Endpoints.PATH)
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

    @PostMapping(path = Endpoints.AUTHENTICATE)
    public CommonVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUsername());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // 5. Set response data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setToken(gameSession.getToken());
            responseVo.setUsername(dto.getUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setOperatorId(dto.getOperatorId());
            responseVo.setTimestamp(System.currentTimeMillis());

        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledGameException disabledGameException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (AuthenticationException playerNotFoundException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (Exception exception) { // any other exception encountered
            httpService.logError(httpRequestLog, exception);
            responseVo.setResponseCode(ResponseCode.ERROR);

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
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), AuthenticationException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
        // Verify operator ID
        ValidationUtils.isEquals(vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator"), dto.getOperatorId(), CredentialNotFoundException::new);
    }

}
