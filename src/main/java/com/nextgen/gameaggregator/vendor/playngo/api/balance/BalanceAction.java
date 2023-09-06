package com.nextgen.gameaggregator.vendor.playngo.api.balance;

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
public class BalanceAction {
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

    @PostMapping(path = EndPoints.BALANCE)
    public String balance(HttpServletRequest request) throws InvalidRequestException, JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        BalanceVo balanceVo = new BalanceVo();
        XmlMapper xmlMapper = new XmlMapper();
        String balanceVoXml;
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            log.info("Balance body: " + body);

            // Convert original request body into commonDto
            BalanceDto balanceDto = xmlMapper.readValue(body, BalanceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify Token
            GameSession gameSession = gameSessionService.verifyToken(balanceDto.getExternalGameSessionId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, balanceDto);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Construct VO
            balanceVo.setReal(balance);
            balanceVo.setStatusCode(ResponseCodes.OK);
        } catch (Exception e) {
            balanceVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, e);
        } finally {
            balanceVoXml = xmlMapper.writeValueAsString(balanceVo);
            balanceVo.setResponseXMLFormat(balanceVoXml);
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVoXml;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, BalanceDto balanceDto) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException {
        // Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), balanceDto.getExternalGameSessionId(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
