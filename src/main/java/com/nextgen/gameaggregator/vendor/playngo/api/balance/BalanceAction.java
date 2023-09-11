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
import com.nextgen.gameaggregator.vendor.playngo.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
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
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.BALANCE)
    public String balance(HttpServletRequest request) throws InvalidRequestException, JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        BalanceVo balanceVo = new BalanceVo();
        XmlMapper xmlMapper = new XmlMapper();
        String balanceVoXml = "";

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            log.info("Balance body: " + body);

            // Convert original request body into commonDto
            BalanceDto balanceDto = xmlMapper.readValue(body, BalanceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Get game session or verify Token
            GameSession gameSession = vendorService.getGameSession(balanceDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, balanceDto);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Construct VO
            balanceVo.setReal(balance);
            balanceVo.setStatusCode(ResponseCodes.OK);

        } catch (InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 CredentialNotFoundException |
                 GameNotSupportedException |
                 JsonProcessingException |
                 InvalidRequestException |
                 NoSuchMethodException |
                 InvocationTargetException |
                 IllegalAccessException internalErrorException) {
            balanceVo.setStatusCodeAndMessage(ResponseCodes.INTERNAL);

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            balanceVo.setStatusCodeAndMessage(ResponseCodes.INVALIDCURRENCY);

        } catch (AuthenticationException authenticationException) {
            balanceVo.setStatusCodeAndMessage(ResponseCodes.SESSIONEXPIRED);

        } catch (Exception exception) {
            balanceVo.setStatusCodeAndMessage(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        } finally {
            try {
                balanceVoXml = xmlMapper.writeValueAsString(balanceVo);
            } catch (JsonProcessingException e) {
                balanceVo.setStatusCode(ResponseCodes.INTERNAL);
            }
            balanceVo.setResponseXMLFormat(balanceVoXml);
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVoXml;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, BalanceDto balanceDto)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            AuthenticationException,
            CredentialNotFoundException,
            GameNotSupportedException {

        // Verify vendor's access token
        vendorService.verifyAccessCode(gameSession.getVendorLineId(), balanceDto);

        // Verify product group id
        vendorService.verifyProductId(gameSession.getVendorLineId(), balanceDto);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, balanceDto.getGameId());

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }

}
