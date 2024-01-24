package com.nextgen.gameaggregator.operator.apiverification.agentinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.AgentIntegrationService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RequestService;
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
public class AgentInfoAction {
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    RequestService requestService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentIntegrationService agentIntegrationService;

    @PostMapping(path = EndPoints.GET_VERIFY_INFO)
    public AgentInfoVo<Object> agentInfo(HttpServletRequest request) {

        AgentInfoVo<Object> agentInfoVo = new AgentInfoVo<>();
        HttpRequestLog httpRequestLog = httpService.start(request);

        if (requestService.isTestEnvironment(profilesActive)) {
            try {
                // Retrieve request body in original string format and convert into dto
                String body = httpRequestLog.getRequestBody();
                AgentInfoDto dto = HttpService.convertJsonToDto(body, AgentInfoDto.class);

                agentInfoVo.setTraceId(dto.getTraceId());
                httpRequestLog.setId(dto.getTraceId());

                // 1. Validate all fields in the request object
                ValidationUtils.validateRequest(dto);

                agentInfoVo = agentApiCredentialService.getAgentApiCredentialForIntegrationTest(dto.getAgentId(), agentInfoVo);

                agentInfoVo = agentIntegrationService.getAgentIntegrationDetails(dto.getAgentId(), agentInfoVo);




            } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
                agentInfoVo.setError("Agent credential not found.");
            } catch (RecordNotFoundException recordNotFoundException) {
                agentInfoVo.setError("Agent integration detail not found");
            } catch (InvalidRequestException invalidRequestException) {
                agentInfoVo.setError("Invalid request, "+ invalidRequestException.getValidation().toString());
            } catch (JsonProcessingException jsonProcessingException) {
                agentInfoVo.setError("Invalid Json request");
            } catch (Exception exception) {
                agentInfoVo.setError(exception.getClass().getName() + ", message :" + exception.getMessage());
                exception.printStackTrace();
            }
        } else {
            agentInfoVo.setError("Invalid environment, only support staging and qa");
        }

        httpService.end(httpRequestLog, agentInfoVo);
        return agentInfoVo;
    }
}
