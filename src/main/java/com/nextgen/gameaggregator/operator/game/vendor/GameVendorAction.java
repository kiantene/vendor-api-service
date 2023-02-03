package com.nextgen.gameaggregator.operator.game.vendor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.custom.IGameVendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.repository.VendorRepository;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class GameVendorAction {
    @Autowired
    private HttpService httpService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private VendorRepository vendorRepository;

    @PostMapping(path = "vendors")
    public OperatorResponseVo< List<IGameVendor>> list(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo< List<IGameVendor> > responseVo = new OperatorResponseVo<>();
        try {

            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            GameVendorDto dto = HttpService.convertJsonToDto(body, GameVendorDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setTraceId(dto.getTraceId());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(Endpoints.HEADER_API_KEY);
            //TODO (by Alex), check agent status
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            System.err.println(apiCredential.getAgent().getId());

            List<IGameVendor> vendorList = vendorRepository.findByAgentSupportedVendorAndStatus(apiCredential.getAgent().getId(),Status.ACTIVE.code);

            System.err.println(vendorList);
          //  GameVendorData GameVendorData =

            responseVo.setData(vendorList);


        } catch (IllegalArgumentException illegalArgumentException) {

        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);
        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
