package com.nextgen.gameaggregator.operator.game.list;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class GameListAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private VendorService vendorService;
    @Autowired
    private LanguageService languageService;
    @Autowired
    private GameListService gameListService;
    @Autowired
    private GameUrlService gameUrlService;

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;

    @PostMapping(path = "list")
    public OperatorResponseVo<GameListData> list(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<GameListData> responseVo = new OperatorResponseVo<>();
        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            GameListDto dto = HttpService.convertJsonToDto(body, GameListDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setId(dto.getTraceId());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

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

            // 5. check if platform supported
            Language language = languageService.checkLanguageCode(dto.getDisplayLanguage());

            // 6. validate agent supported vendor line
            List<AgentVendorLine> agentVendorLines =
                    vendorLineService.getVendorLineByAgent(apiCredential.getAgent(), vendor, currencyIds);
            currencyIds.clear();
            for (AgentVendorLine agentVendorLine : agentVendorLines) {
                currencyIds.add(agentVendorLine.getCurrency().getId());
            }


            GameListData gameListData = gameListService.getGameList(dto, agentVendorLines, vendor, currencyIds, language);
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

//        } catch (InvalidVendorException | InvalidVendorLineException invalidVendorException) {
//            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_VENDOR);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WRONG_CURRENCY);

        } catch (DisabledVendorLineException | DisabledVendorException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LINE_DISABLED);

        } catch (InvalidLanguageException invalidLanguageException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_LANGUAGE);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (InvalidVendorLineException invalidVendorLineException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_CURRENCY_NOT_SUPPORTED);

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
