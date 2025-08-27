package com.nextgen.gameaggregator.operator.apiverification.promo.payout;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutRequest;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.operator.apiverification.wallet.slot.ResponseResultVo;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.API_VERIFY_PATH)
@RequiredArgsConstructor
public class PromoPayoutAction {
    private final RequestService requestService;
    private final HttpService httpService;
    private final AgentApiCredentialService agentApiCredentialService;
    private final ValidationService validationService;
    private final AuthenticationService authenticationService;


    @PostMapping(path = EndPoints.PROMO_PAYOUT)
    public ResponseResultVo<Object> promoPayout(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseResultVo<Object> responseResultVo = new ResponseResultVo<>();
        if (requestService.isTestEnvironment()) {
            try {
                // Retrieve request body in original string format and convert into dto
                PromoPayoutRequest dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), PromoPayoutRequest.class);
                responseResultVo.setRequestBody(dto);

                // 1. Validate all fields in the request object
                ValidationUtils.validateRequest(dto);

                // 2. Check if api key is valid
                String apiKey = request.getHeader(EndPoints.HEADER_API_KEY);
                AgentApiCredential agentApiCredential = validationService.validateApiKey(apiKey);

                String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);
                Map<String, String> headerMap = new HashMap<String, String>();

                //String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
                headerMap.put(EndPoints.HEADER_SIGNATURE, request.getHeader(EndPoints.HEADER_SIGNATURE));
                headerMap.put(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());
                responseResultVo.setRequestHeaders(headerMap);

                responseResultVo.setApiUrl(apiUrl + EndPoints.PROMO_PAYOUT);

                responseResultVo.setRequestStartTime(System.currentTimeMillis());
                ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                        .post()
                        .uri(EndPoints.PROMO_PAYOUT)
                        .header(EndPoints.HEADER_SIGNATURE, request.getHeader(EndPoints.HEADER_SIGNATURE))
                        .header(EndPoints.HEADER_API_KEY, request.getHeader(EndPoints.HEADER_API_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(dto))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                        .toEntity(String.class)
                        .retry(3)
                        .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                        .block();
                responseResultVo.setRequestEndTime(System.currentTimeMillis());

                httpRequestLog.setId(dto.getTraceId());
                responseResultVo.setHttpStatusCode(apiResponse.getStatusCode().value());
                responseResultVo.setResponseBody(apiResponse.getBody().toString());

            } catch (Exception exception) {
                responseResultVo.setError(exception.getClass().getName() + ", message :" + exception.getMessage());
                exception.printStackTrace();
                //throw new RuntimeException(e);
            }
        } else {
            responseResultVo.setError("Invalid environment, only support staging and qa");
        }
        httpService.end(httpRequestLog, responseResultVo);
        return responseResultVo;

    }
}
