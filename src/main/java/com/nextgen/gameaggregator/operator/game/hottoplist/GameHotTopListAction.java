package com.nextgen.gameaggregator.operator.game.hottoplist;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.entity.ga.AgentVendorLine;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.enums.DateRangeType;
import com.nextgen.gameaggregator.enums.HotTopGameType;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.DisabledVendorException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidCurrencyException;
import com.nextgen.gameaggregator.exception.InvalidDateRangeException;
import com.nextgen.gameaggregator.exception.InvalidHotTopGameTypeException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.GameHotTopListService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.VendorService;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class GameHotTopListAction {
	
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private GameHotTopListService gameHotTopListService;
    @Autowired
    private GameUrlService gameUrlService;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;

    @PostMapping(path = "hotTopList")
    public OperatorResponseVo<Object> list(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<Object> responseVo = new OperatorResponseVo<>();
        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            GameHotTopListDto dto = HttpService.convertJsonToDto(body, GameHotTopListDto.class);

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

            // 4. Validate vendor code and vendor supported wallet type
            Vendor vendor = vendorService.verifyVendorByCodeAndWalletType
                    (dto.getVendorCode(), apiCredential.getAgent().getWalletType());

            // 5. Get Agent Supported Currency
            List<Integer> currencyIds = new ArrayList<>();
            if(dto.getCurrency()==null){
                List<AgentCurrency> agentCurrencies = agentApiCredentialService.getAgentSupportedCurrency(apiCredential.getAgent().getId());
                for (AgentCurrency agentCurrency : agentCurrencies) {
                    currencyIds.add(agentCurrency.getCurrency().getId());
                }
            }else{
                Currency currency =  gameUrlService.checkCurrency(dto.getCurrency());
                currencyIds.add(currency.getId());
            }

            // 6. validate agent supported vendor line
            List<AgentVendorLine> agentVendorLines =
                    vendorLineService.getVendorLineByAgent(apiCredential.getAgent(), vendor, currencyIds);
            currencyIds.clear();
            for (AgentVendorLine agentVendorLine : agentVendorLines) {
                currencyIds.add(agentVendorLine.getCurrency().getId());
            }

            Object gameListData = gameHotTopListService.getHotTopGameList(dto, agentVendorLines, vendor, currencyIds);
            responseVo.setData(gameListData);

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

        } catch (DisabledVendorLineException | DisabledVendorException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LINE_DISABLED);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (InvalidVendorLineException invalidVendorLineException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_CURRENCY_NOT_SUPPORTED);

        } catch (InvalidDateRangeException invalidDateRangeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_DATE_RANGE);

        } catch (InvalidHotTopGameTypeException invalidHotTopGameTypeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_GAME_TOPIC);

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
