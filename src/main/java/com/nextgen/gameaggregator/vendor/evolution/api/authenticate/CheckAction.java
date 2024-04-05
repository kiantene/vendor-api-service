package com.nextgen.gameaggregator.vendor.evolution.api.authenticate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolution.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolution.constant.Platforms;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.dto.BasicDto;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CheckAction {
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

    @PostMapping(path = {EndPoints.CHECK, EndPoints.SID})
    public ResponseVo CheckAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CheckDto checkDto = HttpService.convertJsonToDto(body, CheckDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(checkDto);

            GameSession gameSession = null;
            // 2. Verify session token
            if (checkDto.getSid().isBlank()) {
                if (httpRequestLog.getUrl().contains("check")) {
                    throw new InvalidRequestException();
                }
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(checkDto.getUserId());
            } else {
                gameSession = gameSessionService.verifyToken(checkDto.getSid());
            }

            if (httpRequestLog.getUrl().contains("check") && !checkDto.getSid().isBlank() || httpRequestLog.getUrl().contains("sid") && checkDto.getSid().isBlank()) {
                // Regenerate token for session token (launch token only can be use once time)
                if ((checkDto.getChannel().getType().equals("M") && gameSession.getVendorPlatformCode().equals(Platforms.H5.toString())) ||
                        (checkDto.getChannel().getType().equals("P") && gameSession.getVendorPlatformCode().equals(Platforms.WEB.toString()))) {
                    String newToken = UUID.randomUUID().toString();
                    gameSession = gameSessionService.regenerateGameSessionToken(gameSession, newToken);
                }
            }

            this.doVerification(checkDto, gameSession);

            responseVo.setSid(gameSession.getToken());
            responseVo.setUuid(checkDto.getUuid());

        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_SID);

        } catch (JsonProcessingException |
                 InvalidRequestException |
                 InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (DisabledVendorLineException |
                 DisabledGameException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);

        } catch (DisabledAgentPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ACCOUNT_LOCKED);

        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(CheckDto checkDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest((BasicDto) checkDto);
        ValidationUtils.validateRequest(checkDto);
    }

    private void doVerification(CheckDto checkDto, GameSession gameSession)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), checkDto.getUserId(), InvalidPlayerException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
