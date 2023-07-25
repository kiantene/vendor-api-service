package com.nextgen.gameaggregator.vendor.playngo.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ReserveAction {
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
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.RESERVE)
    public String reserve(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ReserveVo reserveVo = new ReserveVo();
        XmlMapper xmlMapper = new XmlMapper();
        String authVoXml;
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            ReserveDto reserveDto = xmlMapper.readValue(body, ReserveDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(reserveDto);

            // Verify Token
            GameSession gameSession = gameSessionService.verifyToken(reserveDto.getExternalGameSessionId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, reserveDto);

            // Process Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, reserveDto, body);

            // Construct VO
            reserveVo.setStatusCode(ResponseCodes.OK);
            reserveVo.setStatusMessage("OK");
            reserveVo.setReal(betEvent.getLastBalance().toString());
        } catch (Exception e) {
            reserveVo.setStatusCode(ResponseCodes.INTERNAL);
            reserveVo.setStatusMessage("INTERNAL");
            httpService.logError(httpRequestLog, e);
        } finally {
            authVoXml = xmlMapper.writeValueAsString(reserveVo);
            reserveVo.setResponseXMLFormat(authVoXml);
            httpService.end(httpRequestLog, reserveVo);
        }

        return authVoXml;
    }

    private void doValidation(ReserveDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, ReserveDto reserveDto) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, InvalidPlayerException, CurrencyNotSupportedException, GameNotSupportedException {
        // Verify Username, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), reserveDto.getExternalId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), reserveDto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, reserveDto.getGameId());

        // Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, reserveDto.getExternalId());
    }

}
