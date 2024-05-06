package com.nextgen.gameaggregator.operator.game.recommendationlist;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.DateRangeType;
import com.nextgen.gameaggregator.enums.HotTopGameType;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidCurrencyException;
import com.nextgen.gameaggregator.exception.InvalidDateRangeException;
import com.nextgen.gameaggregator.exception.InvalidHotTopGameTypeException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.exception.InvalidVendorException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.GameHotTopListService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class GameRecommendationListAction {
	
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private GameHotTopListService gameHotTopListService;
    @Autowired
    private GameUrlService gameUrlService;

    @PostMapping(path = "recommendation")
    public OperatorResponseVo<List<GameRecommendationListData>> list(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<List<GameRecommendationListData>> responseVo = new OperatorResponseVo<>();
        try {
        	
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            GameRecommendationListDto dto = HttpService.convertJsonToDto(body, GameRecommendationListDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setId(dto.getTraceId());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);
            
            if (!dto.getDateRangeType().contains(DateRangeType.MONTHLY.toString().toLowerCase())
            		&& !dto.getDateRangeType().contains(DateRangeType.WEEKLY.toString().toLowerCase())) {
            	throw new InvalidDateRangeException();
            } 
            
            if (!dto.getType().contains(HotTopGameType.HOT.toString().toLowerCase())
            		&& !dto.getType().contains(HotTopGameType.TOP.toString().toLowerCase())) {
            	throw new InvalidHotTopGameTypeException();
            } 

            // 2. Check if api key is valid
            String apiKey = request.getHeader(EndPoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            // 3. Validate the signature
            String signature = request.getHeader(EndPoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 4. Get Agent Supported Currency
            Integer currencyIds;
            Currency currency =  gameUrlService.checkCurrency(dto.getCurrency());
            currencyIds = currency.getId();

            List<GameRecommendationListData> gameDataList = gameHotTopListService.getHotTopGameList(dto, currencyIds);
 
            responseVo.setData(gameDataList);

        } catch (IllegalArgumentException illegalArgumentException) {
            // thrown when any field encountered type mismatch during conversion from json to dto
            log.error(illegalArgumentException.toString());
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WRONG_CURRENCY);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (InvalidDateRangeException invalidDateRangeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_DATE_RANGE);

        } catch (InvalidHotTopGameTypeException invalidHotTopGameTypeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_GAME_TOPIC);

        } catch (InvalidVendorException invalidVendorException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_VENDOR);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);
            exception.printStackTrace();

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);

        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;

    }

}
