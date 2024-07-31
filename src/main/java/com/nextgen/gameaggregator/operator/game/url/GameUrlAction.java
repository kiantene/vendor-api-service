package com.nextgen.gameaggregator.operator.game.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class GameUrlAction {
    public static final String REQUEST_TYPE = "GameUrl";
    private final HttpService httpService;
    private final ValidationService validationService;
    private final GameUrlService gameUrlService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final GameCategoryService gameCategoryService;
    private final LanguageService languageService;
    private final VendorGameService vendorGameService;
    private final LoggingService loggingService;
    private final VendorGameDeactivatedService vendorGameDeactivatedService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;

    @Autowired
    public GameUrlAction(HttpService httpService,
                         ValidationService validationService,
                         GameUrlService gameUrlService,
                         VendorLineService vendorLineService,
                         GameSessionService gameSessionService,
                         VendorService vendorService,
                         GameCategoryService gameCategoryService,
                         LanguageService languageService,
                         VendorGameService vendorGameService,
                         LoggingService loggingService,
                         VendorGameDeactivatedService vendorGameDeactivatedService,
                         WarehouseBetHistoryService warehouseBetHistoryService) {

        this.httpService = httpService;
        this.validationService = validationService;
        this.gameUrlService = gameUrlService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.gameCategoryService = gameCategoryService;
        this.languageService = languageService;
        this.vendorGameService = vendorGameService;
        this.loggingService = loggingService;
        this.vendorGameDeactivatedService = vendorGameDeactivatedService;
        this.warehouseBetHistoryService = warehouseBetHistoryService;
    }

    @PostMapping(path = "url")
    public OperatorResponseVo<GameUrlData> url(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestType(REQUEST_TYPE);
        OperatorResponseVo<GameUrlData> responseVo = new OperatorResponseVo<>();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            GameUrlDto dto = HttpService.convertJsonToDto(body, GameUrlDto.class);
            String traceId = dto.getTraceId();
            responseVo.setTraceId(traceId);
            httpRequestLog.setOperatorUsername(dto.getUsername());

            // 1. Validate all fields in the request object
            loggingService.logStart();
            ValidationUtils.validateRequest(dto);
            loggingService.logProcessTime("gameUrl ｜ ValidationUtils.validateRequest", traceId);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(EndPoints.HEADER_API_KEY);
            loggingService.logStart();
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);
            Agent agent = apiCredential.getAgent();
            Integer agentId = agent.getId();
            httpRequestLog.setAgentId(agentId);
            loggingService.logProcessTime("gameUrl ｜ validationService.validateApiKey", traceId);

            // 3. Validate the signature
            String signature = request.getHeader(EndPoints.HEADER_SIGNATURE);
            loggingService.logStart();
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);
            loggingService.logProcessTime("gameUrl ｜ validationService.validateSignature", traceId);

            // 4. Check if trace Id has been sent before
            loggingService.logStart();
            gameUrlService.checkDuplicateRequest(agentId, dto.getTraceId());
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkDuplicateRequest", traceId);

            // 5.1 Check if Currency exist
            loggingService.logStart();
            Currency currency = gameUrlService.checkCurrency(dto.getCurrency());
            // 5.2 Check if Agent Currency supported
            AgentCurrency agentCurrency = gameUrlService.checkAgentCurrencySupported(agent, currency);
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkAgentCurrencySupported", traceId);

            // 6. check if platform supported
            loggingService.logStart();
            Platform platform = gameUrlService.checkPlatformCode(dto.getPlatform());
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkPlatformCode", traceId);

            // 7. check if platform supported
            loggingService.logStart();
            Language language = languageService.checkLanguageCode(dto.getLanguage());
            loggingService.logProcessTime("gameUrl ｜ languageService.checkLanguageCode", traceId);

            // 8. Check if game is supported
            loggingService.logStart();
            VendorGame vendorGame = vendorGameService.checkGameSupported(dto.getGameCode());
            Integer vendorId = vendorGame.getVendorId();
            Vendor vendor = vendorService.getById(vendorId);
            httpRequestLog.setVendorId(vendorId);
            GameCategory gameCategory = gameCategoryService.getByGameCategoryId(vendorGame.getGameCategoryId(), null);
            loggingService.logProcessTime("gameUrl ｜ vendorGameService.checkGameSupported", traceId);

            // 9 Check if game details is supported (platform, language, currency)
            loggingService.logStart();
            VendorGameCode vendorGameCode = gameUrlService.checkGameDetailSupported(
                    vendorGame, language, platform, agentCurrency.getCurrency());
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkGameDetailSupported", traceId);

            // Check if is game deactivated (agent, masterAgent, house level)
            loggingService.logStart();
            vendorGameDeactivatedService.checkGameSupported(agent, vendorGame.getId());
            loggingService.logProcessTime("gameUrl ｜ vendorGameDeactivatedService.checkGameSupported", traceId);

            // 10. Retrieve vendor line credentials by category
            loggingService.logStart();
            VendorLine vendorLine = vendorLineService.findAgentVendorLine(agent, vendor, agentCurrency.getCurrency(), gameCategory);
            loggingService.logProcessTime("gameUrl ｜ vendorLineService.findAgentVendorLine", traceId);

            // 11. get vendor line credential
            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);

            // 12. check if vendor language supported
            loggingService.logStart();
            VendorLanguageCode vendorLanguageCode = vendorService.findVendorLanguageCode(vendorId, language);
            loggingService.logProcessTime("gameUrl ｜ vendorService.findVendorLanguageCode", traceId);

            // 13. check if vendor currency supported
            loggingService.logStart();
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(vendorId, agentCurrency.getCurrency().getId());
            loggingService.logProcessTime("gameUrl ｜ vendorService.findVendorCurrency", traceId);

            // 14. check if vendor platform supported
            loggingService.logStart();
            String vendorPlatformCode = gameUrlService.getVendorPlatformCode(vendor.getClassName(), vendorGameCode.getPlatformId());
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.getVendorPlatformCode", traceId);

            // 15. Check if Agent player account exists
            loggingService.logStart();
            AgentPlayer agentPlayer = gameUrlService.checkAgentPlayer(agent, dto.getUsername());
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkAgentPlayer", traceId);

            // 16. Check if Vendor player account exists
            loggingService.logStart();
            VendorPlayer vendorPlayer = gameUrlService.checkVendorPlayer(agentPlayer, vendorLine, agentCurrency.getCurrency());
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkVendorPlayer", traceId);

            // 17. create game session in cache
            loggingService.logStart();
            GameSession gameSession = gameUrlService.createGameSession(agentPlayer, vendorPlayer, vendorLine);
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.createGameSession", traceId);

            // 18. save game session into DB
            loggingService.logStart();
            gameSession = gameSessionService.createSession(
                    gameSession, dto, vendorGame, vendorGameCode, agentCurrency.getCurrency(),
                    vendorCurrency, vendorLanguageCode, vendorPlatformCode, dto.getLobbyUrl(), dto.getIpAddress());
            loggingService.logProcessTime("gameUrl ｜ gameSessionService.createSession", traceId);

            // setGameSessionInfo
            httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
            httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());
            httpRequestLog.setGameToken(gameSession.getToken());

            // 16. Request game url from vendor
            GameUrlData gameUrlData = gameUrlService.getGameUrl(vendorGame, gameSession, lineCredentials, vendorLine, httpRequestLog);
            warehouseBetHistoryService.setWarehouseBetHistoryInfoCache(vendorGame, currency);
            responseVo.setData(gameUrlData);

        } catch (IllegalArgumentException illegalArgumentException) {
            log.error(illegalArgumentException.toString());
            httpService.logError(httpRequestLog, illegalArgumentException);
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);

        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);

        } catch (InvalidSignatureException invalidSignatureException) {
            httpService.logError(httpRequestLog, invalidSignatureException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (DuplicateRequestException duplicateRequestException) {
            httpService.logError(httpRequestLog, duplicateRequestException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_DUPLICATE_REQUEST);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            httpService.logError(httpRequestLog, invalidCurrencyException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_WRONG_CURRENCY);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            httpService.logError(httpRequestLog, currencyNotSupportedException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_CURRENCY_NOT_SUPPORTED);

        } catch (InvalidPlatformException invalidPlatformException) {
            httpService.logError(httpRequestLog, invalidPlatformException);
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_PLATFORM);

        } catch (InvalidLanguageException invalidLanguageException) {
            httpService.logError(httpRequestLog, invalidLanguageException);
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_LANGUAGE);

        } catch (GameNotSupportedException gameNotSupportedException) {
            httpService.logError(httpRequestLog, gameNotSupportedException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_GAME);

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            responseVo.setStatus(ResponseCodes.Status.SC_GAME_DISABLED);

        } catch (GameCurrencyNotSupportException gameCurrencyNotSupportException) {
            httpService.logError(httpRequestLog, gameCurrencyNotSupportException);
            responseVo.setStatus(ResponseCodes.Status.SC_GAME_CURRENCY_NOT_SUPPORTED);

        } catch (InvalidVendorLineException vendorLineNotFoundException) {
            httpService.logError(httpRequestLog, vendorLineNotFoundException);
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_VENDOR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            httpService.logError(httpRequestLog, disabledVendorLineException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LINE_DISABLED);

        } catch (VendorLanguageNotSupportedException vendorLanguageNotSupportedException) {
            httpService.logError(httpRequestLog, vendorLanguageNotSupportedException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LANGUAGE_NOT_SUPPORTED);

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            httpService.logError(httpRequestLog, vendorCurrencyNotSupportException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_CURRENCY_NOT_SUPPORTED);

        } catch (VendorPlatformNotSupportedException vendorPlatformNotSupportedException) {
            httpService.logError(httpRequestLog, vendorPlatformNotSupportedException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_PLATFORM_NOT_SUPPORTED);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            httpService.logError(httpRequestLog, disabledAgentPlayerException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_USER_DISABLED);

        } catch (InvalidVendorResponseException invalidVendorResponseException) {
            httpService.logError(httpRequestLog, invalidVendorResponseException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_ERROR);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);
            httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }
}
