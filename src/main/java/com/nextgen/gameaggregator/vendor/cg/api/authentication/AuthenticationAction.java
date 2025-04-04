package com.nextgen.gameaggregator.vendor.cg.api.authentication;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticationAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;

    @Autowired
    public AuthenticationAction(HttpService httpService,
                                GameSessionService gameSessionService,
                                VendorLineService vendorLineService,
                                AgentPlayerService agentPlayerService,
                                VendorGameService vendorGameService,
                                VendorService vendorService) {

        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.AUTHENTICATION)//, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        AuthenticationVo authenticationVo = new AuthenticationVo();
        CommonDto dto = new CommonDto();

        try {
            //convert to dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, CommonDto.class);
            dto.setData(VendorService.urlDecode(dto.getData()));

            //validation
            ValidationUtils.validateRequest(dto);

            //decrypt token return from vendor
            String decryptedToken = vendorService.decryptData(dto.getData(), dto.getChannelId());//we get the json here
            httpRequestLog.setRequestBody(decryptedToken);
            AuthenticationDto authenticationDto = HttpService.convertJsonToDto(decryptedToken, AuthenticationDto.class);

            //validation
            this.doValidation(authenticationDto);

            //get the respective game session with the decrypted token
            GameSession gameSession = gameSessionService.verifyToken(authenticationDto.getToken());

            //verify the status of the session
            this.doVerification(gameSession);

            //set values
            authenticationVo.setChannelId(dto.getChannelId());
            authenticationVo.setAccountId(gameSession.getVendorPlayerUsername());
            authenticationVo.setNickName(gameSession.getAgentPlayerUsername());
            authenticationVo.setErrorCode(ResponseCodes.SUCCESS);
        } catch (InvalidRequestException e) {
            authenticationVo.setErrorCode(ResponseCodes.WRONG_URL);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            authenticationVo.setErrorCode(ResponseCodes.INPUT_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            authenticationVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            try {
                String jsonString = new Gson().toJson(authenticationVo);
                authenticationVo.setEncrypt(vendorService.encryptResponse(jsonString, dto.getChannelId())); //encrypt the whole vo include error
                httpService.end(httpRequestLog, authenticationVo);
            } catch (CredentialNotFoundException e) {
                httpService.logError(httpRequestLog, e);
            }
        }
        //return encrypted string only
        return authenticationVo.getEncrypt();
    }


    private void doValidation(AuthenticationDto dto) throws InvalidRequestException {
        //general validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
