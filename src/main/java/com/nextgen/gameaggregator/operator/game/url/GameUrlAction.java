package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping(path = "url")
    public OperatorResponseVo<GameUrlData> url(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<GameUrlData> responseVo = new OperatorResponseVo<>();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original json string into dto
            GameUrlDto dto = HttpService.convertJsonToDto(body, GameUrlDto.class);
            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setTraceId(dto.getTraceId());
            log.info(dto.toString());

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
            VendorGame vendorGame = gameUrlService.checkGameSupported(dto.getGameCode());

            // TODO: to check available platform

            Integer agentId = apiCredential.getAgent().getId();
            Integer vendorId = vendorGame.getVendorId();
            Currency currency = apiCredential.getAgent().getCurrency();

            // 6. Check if trace Id has been sent before
            gameUrlService.checkDuplicateRequest(agentId, dto.getTraceId());

            // 7. Retrieve vendor line credentials
            VendorLine vendorLine = vendorLineService.getVendorLineByAgent(agentId, vendorId, currency.getId());
            Map<String, String> lineCredentials = vendorLineService.toCredentialMap(vendorLine);

            // 8. Check if vendor player account exists
            GameSession gameSession = gameUrlService.checkPlayer(agentId, dto.getUsername(), vendorLine);
            gameSessionService.createSession(gameSession, dto, vendorGame, currency);
            log.info(gameSession.toString());

            // 9. Request game url from vendor
            GameUrlData gameUrlData = gameUrlService.getGameUrl(vendorGame, gameSession, lineCredentials);
            responseVo.setData(gameUrlData);

        } catch (IllegalArgumentException illegalArgumentException) {
            // thrown when any field encountered type mismatch during conversion from json to dto
            log.error(illegalArgumentException.toString());
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (AuthenticationException authenticationException) {
            responseVo.setStatus(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_GAME);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setStatus(ResponseCodes.Status.SC_CURRENCY_NOT_SUPPORTED);

        } catch (DuplicateRequestException duplicateRequestException) {
            responseVo.setStatus(ResponseCodes.Status.SC_DUPLICATE_REQUEST);

        } catch (NoAvailableLineException noAvailableLineException) {
            responseVo.setStatus(ResponseCodes.Status.SC_UNDER_MAINTENANCE);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);
            exception.printStackTrace();

        } finally {
            responseVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getStatus()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }
}
