package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ReleaseAction {
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

    @PostMapping(path = EndPoints.RELEASE)
    public String release(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ReleaseVo reserveVo = new ReleaseVo();
        XmlMapper xmlMapper = new XmlMapper();
        String authVoXml;
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            ReleaseDto releaseDto = xmlMapper.readValue(body, ReleaseDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(releaseDto);

            // Verify Token
            GameSession gameSession = gameSessionService.verifyToken(releaseDto.getExternalGameSessionId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, releaseDto);

            // Process Bet Result
            ResultType resultType = vendorService.calculateResultType(releaseDto.getBetAmount(), releaseDto.getWinAmount(), releaseDto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, releaseDto, resultType, vendorService, httpRequestLog);

            // Construct VO
            reserveVo.setStatusCode(ResponseCodes.OK);
            reserveVo.setStatusMessage("OK");
            reserveVo.setReal(balance.toString());
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

    private void doValidation(ReleaseDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, ReleaseDto releaseDto) throws InvalidPlayerException, CurrencyNotSupportedException, GameNotSupportedException {
        // Verify Username, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), releaseDto.getExternalId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), releaseDto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, releaseDto.getGameId());
    }

}
