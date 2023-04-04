package com.nextgen.gameaggregator.operator.game.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class GameUrlAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private GameUrlService gameUrlService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private VendorGameService vendorGameService;

    @PostMapping(path = "url")
    public OperatorResponseVo<GameUrlData> url(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<GameUrlData> responseVo = new OperatorResponseVo<>();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            GameUrlDto dto = HttpService.convertJsonToDto(body, GameUrlDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setTraceId(dto.getTraceId());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(Endpoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            // 3. Validate the signature
            String signature = request.getHeader(Endpoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 4. Check if currency is supported
            gameUrlService.checkCurrencySupported(apiCredential.getAgent().getCurrency(), dto.getCurrency());

            // 5. Check if game is supported
            VendorGame vendorGame = vendorGameService.checkGameSupported(dto.getGameCode());

            // 6 Check if game details is supported (platform, language, and status)
            VendorGameCode vendorGameCode = gameUrlService.checkGameDetailSupported(vendorGame.getId(), dto.getPlatform(), dto.getLanguage());

            Integer agentId = apiCredential.getAgent().getId();
            Integer vendorId = vendorGame.getVendor().getId();
            Integer gameCategoryId = vendorGame.getGameCategory().getId();
            Currency currency = apiCredential.getAgent().getCurrency();

            // 7. check if the language is supported by vendor
            String vendorLanguageCode = gameUrlService.checkVendorLanguageSupported(vendorId, vendorGameCode.getLanguageId());

            // 8. Check if trace Id has been sent before
            gameUrlService.checkDuplicateRequest(agentId, dto.getTraceId());

            // 9. Retrieve vendor line credentials by category
            VendorLine vendorLine = vendorLineService.getVendorLineByAgentAndGameCategory(apiCredential.getAgent(), vendorGame.getVendor(), currency.getId(), gameCategoryId);
            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);

            // 10. Check if Vendor Line currency is supported
            VendorLineCurrency vendorLineCurrency = vendorLineService.checkVendorLineSupportedCurrency(vendorLine.getId(), currency.getId());

            String vendorPlatformCode = gameUrlService.getVendorPlatformCode(vendorLine.getVendor().getClassName(), vendorGameCode.getPlatformId());

            // 11. Check if vendor player account exists
            GameSession gameSession = gameUrlService.checkPlayer(agentId, dto.getUsername(), vendorLine, currency.getId());

            gameSession = gameSessionService.createSession(gameSession, dto, vendorGame, vendorGameCode, currency, vendorLineCurrency, vendorLanguageCode, vendorPlatformCode);
            gameSessionService.createSessionByVendorPlayer(gameSession);
            
            // 12. Request game url from vendor
            GameUrlData gameUrlData = gameUrlService.getGameUrl(vendorGame, gameSession, lineCredentials, vendorLine);
            responseVo.setData(gameUrlData);

        } catch (IllegalArgumentException illegalArgumentException) {
            // thrown when any field encountered type mismatch during conversion from json to dto
            log.error(illegalArgumentException.toString());
            responseVo.setResponseCode(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_GAME);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_CURRENCY_NOT_SUPPORTED);

        } catch (VendorLanguageNotSupportedException vendorLanguageNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LANGUAGE_NOT_SUPPORTED);

        } catch (DuplicateRequestException duplicateRequestException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_DUPLICATE_REQUEST);

        } catch (NoAvailableLineException noAvailableLineException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNDER_MAINTENANCE);

        } catch (InvalidVendorLineException invalidVendorLineException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_VENDOR);

        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);

        } catch (ClassNotFoundException
                 | InvocationTargetException
                 | NoSuchMethodException
                 | IllegalAccessException
                 | InstantiationException gameClassError) {
            //all the exception related to vendor game url service
            httpService.logError(httpRequestLog, gameClassError);
            //TODO (by Alex), the correct code to response to operator
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNDER_MAINTENANCE);

        } catch (InvalidVendorResponseException invalidVendorResponseException) {
            httpService.logError(httpRequestLog, invalidVendorResponseException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_ERROR);

        } catch (InvalidFormatException invalidFormatException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);

        } catch (InvalidVendorException invalidVendorException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_VENDOR);

        }

//        catch (Exception exception) {
//            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
//            httpService.logError(httpRequestLog, exception);
//            exception.printStackTrace();
//        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
