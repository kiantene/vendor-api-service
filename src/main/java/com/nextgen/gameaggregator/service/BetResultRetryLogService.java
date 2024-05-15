package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RawBetResultRetryLog;
import com.nextgen.gameaggregator.enums.RetryStatus;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetResultRetryLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;


@Service
@Slf4j
public class BetResultRetryLogService {
    @Autowired
    private RawBetResultRetryLogRepository rawBetResultRetryLogRepository;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RequestService requestService;

    public void create(HttpRequestLog httpRequestLog, Integer vendorId, Integer agentId, BetInformation betInformation, String action) {
        RawBetResultRetryLog rawBetResultRetryLog = new RawBetResultRetryLog();
        Integer defaultRetryCounter = 1;
        Long nextRetryTime = System.currentTimeMillis();

        rawBetResultRetryLog.setId(betInformation.getBetId() + action);
        rawBetResultRetryLog.setTransactionId(betInformation.getInternalTransactionId());
        rawBetResultRetryLog.setAction(action);
        rawBetResultRetryLog.setVendorId(vendorId);
        rawBetResultRetryLog.setAgentId(agentId);
        rawBetResultRetryLog.setOperatorData(httpRequestLog.getOperatorData());
        rawBetResultRetryLog.setRetryCounter(defaultRetryCounter);
        rawBetResultRetryLog.setNextRetryTime(this.calculateNextRetryTime(defaultRetryCounter, nextRetryTime));
        rawBetResultRetryLog.setStatus(RetryStatus.FAILED.code);
        rawBetResultRetryLog.setCreateDate(nextRetryTime);

        rawBetResultRetryLogRepository.save(rawBetResultRetryLog);

    }

    public Long calculateNextRetryTime(Integer retryCounter, Long nextRetryTime) {
        Integer maxRetryCounter = 6;
        Integer totalDelaySeconds = 30000;

        for (Integer i = 1; i <= maxRetryCounter; i++) {
            totalDelaySeconds = i * totalDelaySeconds;

            if (retryCounter == i) {
                break;
            }
        }

        nextRetryTime = nextRetryTime + totalDelaySeconds;

        return nextRetryTime;
    }

    public void call(String operatorData, String action, Integer agentId, HttpRequestLog httpRequestLog) throws Exception, InvalidFormatException {

        try {
            MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
            WalletBalanceVo responseVo;
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);

            String traceId = UUID.randomUUID().toString();
            String updatedOperatorData = this.updateOperatorDataWithNewTraceId(operatorData, traceId);
            String apiUrl = agentApiCredential.getCallbackUrl();
            String signature = authenticationService.generateSignatureWithJson(updatedOperatorData, agentApiCredential.getApiSecret());

            headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
            headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());
            httpRequestLog.setOperatorData(updatedOperatorData);
            httpRequestLog.setOperatorEndPoints(apiUrl + action);

            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(action)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(updatedOperatorData))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                    .block();

            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);
            httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());
            httpRequestLog.setOperatorResponse(apiResponse.getBody());
            httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            requestService.operatorStatusException(responseVo.getStatus());

        } catch (InvalidFormatException | InvalidAgentApiCredentialException e) {
            throw new InvalidFormatException(e.getMessage());

        } catch (InvalidOperatorResponseException e) {
            throw new InvalidOperatorResponseException(e.getMessage(), ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (Exception e) {
            throw new Exception(e);
        }

    }

    public String updateOperatorDataWithNewTraceId(String operatorData, String traceId) throws InvalidFormatException {

        try {
            // Create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse JSON string to JsonNode
            JsonNode jsonNode = objectMapper.readTree(operatorData);

            // Update traceId value
            ((ObjectNode) jsonNode).put("traceId", traceId);

            // Convert JsonNode back to JSON string
            operatorData = objectMapper.writeValueAsString(jsonNode);

        } catch (Exception exception) {
            throw new InvalidFormatException();
        }

        return operatorData;

    }

    public HttpRequestLog defaultHttpRequestLogForProcessRetryRequest() {

        HttpRequestLog httpRequestLog = new HttpRequestLog();

        httpRequestLog.setId(UUID.randomUUID().toString());
        httpRequestLog.setUrl("BetResultRetryScheduler");
        httpRequestLog.setMethod("JAVA SCHEDULER");

        return httpRequestLog;
    }
}
