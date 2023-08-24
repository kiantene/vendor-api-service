package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playngo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playngo.constant.ResponseCodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    @PostMapping(path = EndPoints.AUTHTHENTICATE)
    public String authenticate(HttpServletRequest request) throws InvalidRequestException, JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        AuthVo authVo = new AuthVo();
        XmlMapper xmlMapper = new XmlMapper();
        String authVoXml;
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            AuthDto authDto = xmlMapper.readValue(body, AuthDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(authDto);

            // Verify Token
            GameSession gameSession = gameSessionService.verifyToken(authDto.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, authDto);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Construct VO
            authVo.setExternalId(gameSession.getVendorPlayerUsername());
            authVo.setStatusCode(ResponseCodes.OK);
            authVo.setStatusMessage("OK");
            authVo.setUserCurrency(gameSession.getVendorCurrencyCode());
            authVo.setReal(balance.toString());
            authVo.setExternalGameSessionId(gameSession.getToken());
        } catch (Exception e) {
            authVo.setStatusCode(ResponseCodes.INTERNAL);
            authVo.setStatusMessage("INTERNAL");
            httpService.logError(httpRequestLog, e);
        } finally {
            authVoXml = xmlMapper.writeValueAsString(authVo);
            authVo.setResponseXMLFormat(authVoXml);
            httpService.end(httpRequestLog, authVo);
        }

        return authVoXml;
    }

    private void doValidation(AuthDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, AuthDto authDto) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException {
        // Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), authDto.getUsername(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
