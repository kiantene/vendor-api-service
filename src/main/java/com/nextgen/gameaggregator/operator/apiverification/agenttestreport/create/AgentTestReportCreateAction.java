package com.nextgen.gameaggregator.operator.apiverification.agenttestreport.create;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.AgentIntegrationService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.API_VERIFY_PATH)
@Slf4j
public class AgentTestReportCreateAction {
    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    RequestService requestService;
    @Autowired
    private HttpService httpService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private AgentIntegrationService agentIntegrationService;

    @PostMapping(path = EndPoints.CREATE_VERIFY_REPORT)
    public OperatorResponseVo detail(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo responseVo = new OperatorResponseVo<>();
        if (requestService.isTestEnvironment(profilesActive)) {
            try {
                String body = httpRequestLog.getRequestBody();
                AgentTestReportCreateDto dto = HttpService.convertJsonToDto(body, AgentTestReportCreateDto.class);

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

                agentIntegrationService.updateAgentIntegrationDetails(apiCredential.getAgent().getId(), dto.getUsername(), dto.getGameCode());

                agentIntegrationService.CreateAndFlushNewAgentStepReport(apiCredential.getAgent().getId());



//        } catch (Exception exception) {
//            responseVo.setResponseCode(ResponseCodes.Status.SC_UNKNOWN_ERROR);
//            httpService.logError(httpRequestLog, exception);
//            exception.printStackTrace();
            } catch (InvalidSignatureException invalidSignatureException) {
                responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);
            } catch (AuthenticationException authenticationException) {
                responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);
            } catch (InvalidRequestException invalidRequestException) {
                responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);
                responseVo.setValidation(invalidRequestException.getValidation());
            } catch (JsonProcessingException jsonProcessingException) {
                responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);
            } finally {
                responseVo.setMessage(responseVo.getStatus().description);
            }
        } else {

        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
