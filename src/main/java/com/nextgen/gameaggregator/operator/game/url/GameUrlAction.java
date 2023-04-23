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

import jakarta.servlet.http.HttpServletRequest;
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
    private VendorService vendorService;

    @Autowired
    private LanguageService languageService;
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

            // 4. Check if trace Id has been sent before
            gameUrlService.checkDuplicateRequest(apiCredential.getAgent().getId(), dto.getTraceId());

            // 5. Check if Agent currency is supported
            gameUrlService.checkAgentCurrencySupported(apiCredential.getAgent().getCurrency(), dto.getCurrency());

            // 6. check if platform supported
            Platform platform = gameUrlService.checkPlatformCode(dto.getPlatform());

            // 7. check if platform supported
            Language language = languageService.checkLanguageCode(dto.getLanguage());

            // 8. Check if game is supported
            VendorGame vendorGame = vendorGameService.checkGameSupported(dto.getGameCode());

            // 9 Check if game details is supported (platform, language, currency)
            VendorGameCode vendorGameCode = gameUrlService.checkGameDetailSupported(
                    vendorGame, language, platform, apiCredential.getAgent().getCurrency());

            // 10. Retrieve vendor line credentials by category
            VendorLine vendorLine = vendorLineService.findAgentVendorLine(
                    apiCredential.getAgent(), vendorGame.getVendor(), apiCredential.getAgent().getCurrency(), vendorGame.getGameCategory());

            // 11. get vendor line credential
            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);

            // 12. check if vendor language supported
            VendorLanguageCode vendorLanguageCode = vendorService.findVendorLanguageCode(vendorLine.getVendor(), language);

            // 13. check if vendor currency supported
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(vendorLine.getVendor(), apiCredential.getAgent().getCurrency());

            // 14. check if vendor platform supported
            String vendorPlatformCode = gameUrlService.getVendorPlatformCode(vendorLine.getVendor().getClassName(), vendorGameCode.getPlatformId());

            // 15. Check if vendor player account exists
            GameSession gameSession = gameUrlService.checkPlayer(apiCredential.getAgent(), dto.getUsername(), vendorLine, apiCredential.getAgent().getCurrency());

            gameSession = gameSessionService.createSession(
                    gameSession, dto, vendorGame, vendorGameCode, apiCredential.getAgent().getCurrency(), vendorCurrency, vendorLanguageCode, vendorPlatformCode);

            gameSessionService.createSessionByVendorPlayer(gameSession);

            // 16. Request game url from vendor
            GameUrlData gameUrlData = gameUrlService.getGameUrl(vendorGame, gameSession, lineCredentials, vendorLine);
            responseVo.setData(gameUrlData);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error(illegalArgumentException.toString());
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.printStackTrace();
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (DuplicateRequestException duplicateRequestException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_DUPLICATE_REQUEST);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_CURRENCY_NOT_SUPPORTED);

        } catch (InvalidPlatformException invalidPlatformException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_PLATFORM);

        } catch (InvalidLanguageException invalidLanguageException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_LANGUAGE);

        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_GAME);

        } catch (DisabledGameException disabledGameException) {
            responseVo.setStatus(ResponseCodes.Status.SC_GAME_DISABLED);

        } catch (GamePlatformNotSupportException gamePlatformNotSupportException) {
            responseVo.setStatus(ResponseCodes.Status.SC_GAME_PLATFORM_NOT_SUPPORTED);

        } catch (GameCurrencyNotSupportException gameCurrencyNotSupportException) {
            responseVo.setStatus(ResponseCodes.Status.SC_GAME_CURRENCY_NOT_SUPPORTED);

        } catch (GameLanguageNotSupportException gameLanguageNotSupportException) {
            responseVo.setStatus(ResponseCodes.Status.SC_GAME_LANGUAGE_NOT_SUPPORTED);

        } catch (InvalidVendorLineException vendorLineNotFoundException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_VENDOR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LINE_DISABLED);

        } catch (VendorLanguageNotSupportedException vendorLanguageNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LANGUAGE_NOT_SUPPORTED);

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_CURRENCY_NOT_SUPPORTED);

        } catch (VendorPlatformNotSupportedException vendorPlatformNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_PLATFORM_NOT_SUPPORTED);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_USER_DISABLED);

        } catch (InvalidVendorResponseException invalidVendorResponseException) {
            httpService.logError(httpRequestLog, invalidVendorResponseException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_ERROR);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);
            exception.printStackTrace();


        } finally {
            responseVo.setMessage(responseVo.getStatus().description);

        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
