package com.nextgen.gameaggregator.operator.transfer.deposit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "cash/")
@Slf4j
public class DepositAction {

    @Autowired
    private HttpService httpService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private GameUrlService gameUrlService;

    @Autowired
    private TransferIdempotentService transferIdempotentService;

    @Autowired
    private TransferService transferService;

    @PostMapping(path = "deposit")
    public OperatorResponseVo<DepositData> detail(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<DepositData> responseVo = new OperatorResponseVo<>();


        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            DepositDto dto = HttpService.convertJsonToDto(body, DepositDto.class);
            String traceId = dto.getTraceId();
            responseVo.setTraceId(traceId);

            // 1. Validate all fields in the request object
            loggingService.logStart();
            ValidationUtils.validateRequest(dto);


            // 2. Check if api key is valid
            String apiKey = request.getHeader(EndPoints.HEADER_API_KEY);
            loggingService.logStart();
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            // 3. Validate the signature
            String signature = request.getHeader(EndPoints.HEADER_SIGNATURE);
            loggingService.logStart();
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            //4. validate duplicate traceId request
            transferIdempotentService.checkUniqueTraceIdRequest(dto.getTraceId(), apiCredential.getAgent().getId());

            //5. validate duplicate

            // 6.1 Check if Currency exist
            loggingService.logStart();
            Currency currency =  gameUrlService.checkCurrency(dto.getCurrency());
            // 6.2 Check if Agent Currency supported
            AgentCurrency agentCurrency =
                    gameUrlService.checkAgentCurrencySupported(apiCredential.getAgent(), currency);


            transferService.checkAgentPlayer(apiCredential.getAgent(), dto.getUsername(), currency);


            DepositData depositData = new DepositData();
            depositData.setCurrencyCode(dto.getCurrency());
            depositData.setReferenceId(dto.getReferenceId());
            depositData.setUsername(dto.getUsername());
            depositData.setTimestamp(System.currentTimeMillis());
            responseVo.setData(depositData);

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

        } catch (InvalidCurrencyException invalidCurrencyException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_WRONG_CURRENCY);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_CURRENCY_NOT_SUPPORTED);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_USER_DISABLED);


//        } catch (Exception exception) {
//            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
//            httpService.logError(httpRequestLog, exception);
//            exception.printStackTrace();

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);
            httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());

        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
