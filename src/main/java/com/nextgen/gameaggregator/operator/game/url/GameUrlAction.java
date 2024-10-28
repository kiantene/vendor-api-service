package com.nextgen.gameaggregator.operator.game.url;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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

@RestController
@Slf4j
public class GameUrlAction {
    public static final String REQUEST_TYPE = "GameUrl";
    private final HttpService httpService;
    private final AgentService agentService;
    private final AgentProductService agentProductService;
    private final ValidationService validationService;
    private final CurrencyService currencyService;
    private final GameUrlService gameUrlService;
    private final VendorLineService vendorLineService;
    private final ProductVendorLineService productVendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final LanguageService languageService;
    private final VendorGameService vendorGameService;
    private final VendorGameCodeService vendorGameCodeService;
    private final ProductGameService productGameService;
    private final LoggingService loggingService;
    private final VendorGameDeactivatedService vendorGameDeactivatedService;
    private final WarehouseBetHistoryService warehouseBetHistoryService;

    @Autowired
    public GameUrlAction(HttpService httpService,
                         AgentService agentService,
                         AgentProductService agentProductService,
                         ValidationService validationService,
                         CurrencyService currencyService,
                         GameUrlService gameUrlService,
                         VendorLineService vendorLineService,
                         ProductVendorLineService productVendorLineService,
                         GameSessionService gameSessionService,
                         VendorService vendorService,
                         LanguageService languageService,
                         VendorGameService vendorGameService,
                         VendorGameCodeService vendorGameCodeService,
                         ProductGameService productGameService,
                         LoggingService loggingService,
                         VendorGameDeactivatedService vendorGameDeactivatedService,
                         WarehouseBetHistoryService warehouseBetHistoryService) {

        this.httpService = httpService;
        this.agentService = agentService;
        this.agentProductService = agentProductService;
        this.validationService = validationService;
        this.currencyService = currencyService;
        this.gameUrlService = gameUrlService;
        this.vendorLineService = vendorLineService;
        this.productVendorLineService = productVendorLineService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.languageService = languageService;
        this.vendorGameService = vendorGameService;
        this.vendorGameCodeService = vendorGameCodeService;
        this.productGameService = productGameService;
        this.loggingService = loggingService;
        this.vendorGameDeactivatedService = vendorGameDeactivatedService;
        this.warehouseBetHistoryService = warehouseBetHistoryService;
    }

    @PostMapping(path = "game/url")
    public OperatorResponseVo<GameUrlData> url(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestType(REQUEST_TYPE);
        OperatorResponseVo<GameUrlData> responseVo = new OperatorResponseVo<>();

        try {
            // Retrieve request body in original string format and convert into dto
            GameUrlDto dto = this.initRequest(httpRequestLog, responseVo);
            String traceId = dto.getTraceId();

            String apiKey = request.getHeader(EndPoints.HEADER_API_KEY);
            String signature = request.getHeader(EndPoints.HEADER_SIGNATURE);
            GameLaunchDto gameLaunchDto = this.doValidation(dto, apiKey, signature, httpRequestLog);
            Integer agentId = gameLaunchDto.getAgentId();
            Integer currencyId = gameLaunchDto.getCurrencyId();
            Agent agent = agentService.get(agentId);

            // 8. Check if game is supported
            loggingService.logStart();
            VendorGame vendorGame = vendorGameService.checkGameSupported(dto.getGameCode());
            gameLaunchDto.setVendorGameId(vendorGame.getId());
            loggingService.logProcessTime("gameUrl ｜ vendorGameService.checkGameSupported", traceId);

            boolean isLaunchByProductGame = vendorGame.getCreateById() == 10;
            Integer vendorId = vendorGame.getVendorId();
            Integer gameCategoryId = vendorGame.getGameCategoryId();
            VendorLine vendorLine = null;
            Integer platformId = gameLaunchDto.getPlatformId();
            String gameCode = "";

            gameLaunchDto.setVendorId(vendorId);
            gameLaunchDto.setGameCategoryId(gameCategoryId);

            if (isLaunchByProductGame) {

                ProductGame productGame = productGameService.getByCode(dto.getGameCode());
                Integer productId = productGame.getProductId();
                Integer productGameId = productGame.getId();
                gameCategoryId = productGame.getGameCategoryId();
                gameLaunchDto.setProductId(productId);
                gameLaunchDto.setProductGameId(productGameId);
                gameLaunchDto.setVendorId(vendorId);
                gameLaunchDto.setGameCategoryId(gameCategoryId);

                AgentProductDetail agentProductDetail = agentProductService.getProductAgentVendorLine(productId, gameCategoryId, currencyId, agentId);
                Integer vendorLineId = agentProductDetail.getVendorLineId();
                boolean shouldFindPriorityVendorLine = agentProductDetail.getVendorLineId() == null;

                if (shouldFindPriorityVendorLine) {
                    ProductVendorLine productVendorLine = productVendorLineService.getHighestPriorityLine(productId, gameCategoryId, currencyId);

                    vendorId = productVendorLine.getVendorId();
                    vendorLineId = productVendorLine.getVendorLineId();
                    vendorLine = vendorLineService.getVendorLineById(vendorLineId);

                    gameLaunchDto.setVendorLineId(productVendorLine.getVendorLineId());
                } else {
                    gameLaunchDto.setVendorLineId(vendorLineId);
                    vendorLine = vendorLineService.getVendorLineById(vendorLineId);
                    vendorId = vendorLine.getVendorId();
                }
                
                VendorGameCode vendorGameCode = vendorGameCodeService.getByProductGame(productGameId, vendorId, platformId, gameLaunchDto.getLanguageId());
                productGameService.verifyProductGameDeactivated(productGameId, agentId, gameLaunchDto.getMasterAgentId(), gameLaunchDto.getHouseId());
                gameCode = vendorGameCode.getOpenGameCode();

            } else {
                gameLaunchDto.setOpenGameCode(vendorGame.getVendorGameCode());
                gameLaunchDto.setVendorGameId(vendorGame.getId());

                // 9 Check if game details is supported (platform, language, currency)
                loggingService.logStart();
                VendorGameCode vendorGameCode = gameUrlService.checkGameDetailSupported(gameLaunchDto);
                loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkGameDetailSupported", traceId);
                gameCode = vendorGameCode.getOpenGameCode();

                // Check if is game deactivated (agent, masterAgent, house level)
                loggingService.logStart();
                vendorGameDeactivatedService.checkGameSupported(agent, vendorGame.getId());
                loggingService.logProcessTime("gameUrl ｜ vendorGameDeactivatedService.checkGameSupported", traceId);

                // 10. Retrieve vendor line credentials by category
                loggingService.logStart();
                vendorLine = vendorLineService.findAgentVendorLine(agentId, vendorId, currencyId, gameCategoryId);
                loggingService.logProcessTime("gameUrl ｜ vendorLineService.findAgentVendorLine", traceId);
            }

            gameLaunchDto.setOpenGameCode(gameCode);
            httpRequestLog.setVendorId(vendorId);

            // 11. get vendor line credential
            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);

            // 12. check if vendor language supported
            loggingService.logStart();
            VendorLanguageCode vendorLanguageCode = vendorService.findVendorLanguageCode(vendorId, gameLaunchDto.getLanguageId());
            loggingService.logProcessTime("gameUrl ｜ vendorService.findVendorLanguageCode", traceId);

            // 13. check if vendor currency supported
            loggingService.logStart();
            VendorCurrency vendorCurrency = vendorService.findVendorCurrency(vendorId, currencyId);
            loggingService.logProcessTime("gameUrl ｜ vendorService.findVendorCurrency", traceId);

            // 14. check if vendor platform supported
            loggingService.logStart();
            Vendor vendor = vendorService.getById(vendorId);
            String vendorPlatformCode = gameUrlService.getVendorPlatformCode(vendor.getClassName(), platformId);
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.getVendorPlatformCode", traceId);

            // 15. Check if Agent player account exists
            loggingService.logStart();
            AgentPlayer agentPlayer = gameUrlService.checkAgentPlayer(agent, dto.getUsername());
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkAgentPlayer", traceId);
            gameLaunchDto.setAgentPlayerId(agentPlayer.getId());
            gameLaunchDto.setAgentPlayerUsername(agentPlayer.getUsername());

            // 16. Check if Vendor player account exists
            loggingService.logStart();
            VendorPlayer vendorPlayer = gameUrlService.checkVendorPlayer(agentPlayer, vendorLine, currencyId);
            loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkVendorPlayer", traceId);
            gameLaunchDto.setVendorPlayerId(vendorPlayer.getId());
            gameLaunchDto.setVendorPlayerUsername(vendorPlayer.getUsername());
            gameLaunchDto.setVendorLineId(vendorLine.getId());

            String gameSessionToken = UUID.randomUUID().toString();
            GameSession gameSession = gameSessionService.create(gameSessionToken, dto, gameLaunchDto, vendorCurrency, vendorLanguageCode, vendorPlatformCode);

            // setGameSessionInfo
            httpRequestLog.setVendorUsername(gameSession.getVendorPlayerUsername());
            httpRequestLog.setVendorGameCode(gameSession.getVendorGameCode());
            httpRequestLog.setGameToken(gameSession.getToken());

            // 16. Request game url from vendor
            GameUrlData gameUrlData = gameUrlService.getGameUrl(gameCode, gameSession, lineCredentials, vendorLine, httpRequestLog);
            warehouseBetHistoryService.setWarehouseBetHistoryInfoCache(vendorGame, currencyId);
            responseVo.setData(gameUrlData);

        } catch (IllegalArgumentException illegalArgumentException) {
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

        } catch (DisabledVendorLineException | ProductVendorLineNotFoundException disabledVendorLineException) {
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

        } catch (ProductAccessDeniedException productAccessDeniedException) {
            httpService.logError(httpRequestLog, productAccessDeniedException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_PRODUCT_ACCESS_DENIED);

        } catch (ProductCombinationNotSupportedException productCombinationNotSupportedException) {
            httpService.logError(httpRequestLog, productCombinationNotSupportedException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_PRODUCT_COMBINATION_NOT_SUPPORTED);

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

    public GameUrlDto initRequest(HttpRequestLog httpRequestLog, OperatorResponseVo<GameUrlData> responseVo) throws JsonProcessingException {
        String body = httpRequestLog.getRequestBody();
        GameUrlDto dto = HttpService.convertJsonToDto(body, GameUrlDto.class);
        httpRequestLog.setId(dto.getTraceId());
        httpRequestLog.setOperatorUsername(dto.getUsername());
        responseVo.setTraceId(dto.getTraceId());

        return dto;
    }

    public GameLaunchDto doValidation(GameUrlDto dto, String apiKey, String signature, HttpRequestLog httpRequestLog)
            throws InvalidRequestException, AuthenticationException, InvalidSignatureException,
            DuplicateRequestException, InvalidCurrencyException, CurrencyNotSupportedException, InvalidPlatformException, InvalidLanguageException {

        String traceId = dto.getTraceId();
        String body = httpRequestLog.getRequestBody();
        GameLaunchDto gameLaunchDto = new GameLaunchDto();
        gameLaunchDto.setTraceId(traceId);

        // 1. Validate all fields in the request object
        loggingService.logStart();
        ValidationUtils.validateRequest(dto);
        loggingService.logProcessTime("gameUrl ｜ ValidationUtils.validateRequest", traceId);

        // 2. Check if api key is valid
        loggingService.logStart();
        AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);
        Agent agent = apiCredential.getAgent();
        Integer agentId = agent.getId();
        httpRequestLog.setAgentId(agentId);
        gameLaunchDto.setAgentId(agentId);
        gameLaunchDto.setHouseId(agent.getHouseId());
        gameLaunchDto.setMasterAgentId(agent.getMasterAgentId());
        loggingService.logProcessTime("gameUrl ｜ validationService.validateApiKey", traceId);

        // 3. Validate the signature
        loggingService.logStart();
        validationService.validateSignature(body, apiCredential.getApiSecret(), signature);
        loggingService.logProcessTime("gameUrl ｜ validationService.validateSignature", traceId);

        // 4. Check if trace Id has been sent before
        loggingService.logStart();
        gameUrlService.checkDuplicateRequest(agentId, dto.getTraceId()); // to change to requestIdempotentLog
        loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkDuplicateRequest", traceId);

        // 5.1 Check if Currency exist
        Currency currency = currencyService.getByCode(dto.getCurrency());
        Integer currencyId = currency.getId();
        gameLaunchDto.setCurrencyId(currencyId);
        gameLaunchDto.setCurrencyCode(currency.getCode());
        // 5.2 Check if Agent Currency supported
        agentService.isCurrencySupported(agentId, currencyId);

        // 6. check if platform supported
        loggingService.logStart();
        Platform platform = gameUrlService.checkPlatformCode(dto.getPlatform());
        loggingService.logProcessTime("gameUrl ｜ gameUrlService.checkPlatformCode", traceId);
        gameLaunchDto.setPlatformId(platform.getId());

        // 7. check if platform supported
        loggingService.logStart();
        Language language = languageService.checkLanguageCode(dto.getLanguage());
        loggingService.logProcessTime("gameUrl ｜ languageService.checkLanguageCode", traceId);
        gameLaunchDto.setLanguageId(language.getId());

        return gameLaunchDto;
    }
}
