package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playngo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playngo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playngo.constant.Formats;
import com.nextgen.gameaggregator.vendor.playngo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playngo.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.AUTHTHENTICATE)
    public String authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        AuthVo authVo = new AuthVo();
        XmlMapper xmlMapper = new XmlMapper();
        String authVoXml = "";

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            log.info("Playngo Auth body: " + body);

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

            // Get country and region
            String country = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.COUNTRY);
            String region = "";
            try {
                region = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.REGION);
            } catch (CredentialNotFoundException credentialNotFoundException) {
                // do nothing
            }

            // Construct VO
            authVo.setStatusCodeAndMessage(ResponseCodes.OK);
            authVo.setExternalGameSessionId(gameSession.getToken());
            authVo.setExternalId(gameSession.getVendorPlayerUsername());
            authVo.setUserCurrency(gameSession.getVendorCurrencyCode());
            authVo.setRegistration(this.getRegistration());
            authVo.setBirthdate(this.getBirthDate());
            authVo.setCountry(country);
            authVo.setRegion(region);
            authVo.setReal(balance);

        } catch (InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 CredentialNotFoundException |
                 GameNotSupportedException |
                 JsonProcessingException internalErrorException) {
            authVo.setStatusCodeAndMessage(ResponseCodes.INTERNAL);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            authVo.setStatusCodeAndMessage(ResponseCodes.ACCOUNTDISABLED);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                authVo.setStatusCodeAndMessage(
                        invalidRequestException.getValidation()
                                .entrySet()
                                .stream()
                                .findFirst()
                                .map(Map.Entry::getValue) // get the value of the first element
                                .orElse(ResponseCodes.INTERNAL)
                );

            } else {
                authVo.setStatusCodeAndMessage(ResponseCodes.INTERNAL);

            }

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            authVo.setStatusCodeAndMessage(ResponseCodes.INVALIDCURRENCY);

        } catch (AuthenticationException authenticationException) {
            authVo.setStatusCodeAndMessage(ResponseCodes.WRONGUSERNAMEPASSWORD);

        } catch (Exception exception) {
            authVo.setStatusCodeAndMessage(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        } finally {
            try {
                authVoXml = xmlMapper.writeValueAsString(authVo);

            } catch (JsonProcessingException e) {
                authVo.setStatusCodeAndMessage(ResponseCodes.INTERNAL);

            }

            authVo.setResponseXMLFormat(authVoXml);
            httpService.end(httpRequestLog, authVo);

        }

        return authVoXml;
    }

    private void doValidation(AuthDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, AuthDto authDto)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            CredentialNotFoundException,
            AuthenticationException,
            GameNotSupportedException,
            InvalidRequestException {

        // Verify vendor's access token
        vendorService.verifyAccessCode(gameSession.getVendorLineId(), authDto);

        // Verify product group id
        vendorService.verifyProductId(gameSession.getVendorLineId(), authDto);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, authDto.getGameId());

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

    private String getBirthDate() {
        LocalDate currentDate = LocalDate.now();
        LocalDate birthDate = currentDate.minusYears(30);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_FORMAT);
        return birthDate.format(formatter);
    }

    private String getRegistration() {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_FORMAT);
        return currentDate.format(formatter);
    }
}
