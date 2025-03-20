package com.nextgen.gameaggregator.vendor.playtech.api.logout;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playtech.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playtech.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playtech.constant.PrefixConstant;
import com.nextgen.gameaggregator.vendor.playtech.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playtech.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.playtech.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class LogOutAction {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;

    @Autowired
    public LogOutAction(HttpService httpService,
                        VendorService vendorService,
                        GameSessionService gameSessionService,
                        VendorLineService vendorLineService,
                        AgentPlayerService agentPlayerService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
    }


    @PostMapping(path = EndPoints.LOG_OUT)
    public CommonVo logOut(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String removePrefix = PrefixConstant.REMOVE_PREFIX;

        CommonVo responseVo = new CommonVo();
        CommonDto commonDto = new CommonDto();
        try {
            // Log request body
            String body = URLDecoder.decode(httpRequestLog.getRequestBody(), StandardCharsets.UTF_8);

            //Define request body
            commonDto = HttpService.convertJsonToDto(body, CommonDto.class);
            // Validate DTO.
            ValidationUtils.validateRequest(commonDto);

            String removedPrefix = vendorService.removePrefix(commonDto.getExternalToken(), removePrefix);
            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(removedPrefix);
            this.doVerification(commonDto, gameSession);
            if (gameSession.getStatus() != 0) {
                gameSessionService.clearGameSession(gameSession, gameSession.getAgentPlayerUsername(),
                        gameSession.getVendorGameCode());
            } else {
                throw new AuthenticationException();
            }

        } catch (InvalidRequestException e) {
            responseVo.setError(ErrorVo.from(ResponseCodes.ERR_REGULATORY_GENERAL));
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            responseVo.setError(ErrorVo.from(ResponseCodes.ERR_PLAYER_NOT_FOUND));
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setError(ErrorVo.from(ResponseCodes.ERR_AUTHENTICATION_FAILED));
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setError(ErrorVo.from(ResponseCodes.INTERNAL_ERROR));
            httpService.logError(httpRequestLog, e);
        } finally {
            responseVo.setRequestId(commonDto.getRequestId());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doVerification(CommonDto commonDto, GameSession gameSession)
            throws InvalidPlayerException,
            CredentialNotFoundException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledVendorLineException {

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        String kioskPrefix = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.KIOSK_PREFIX);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(kioskPrefix + "_" + gameSession.getVendorPlayerUsername(),
                commonDto.getUserName(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

    }

}
