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
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BetResultRetryLogService {
    public static final Integer maxRetryCounter = 7;
    private final RawBetResultRetryLogRepository rawBetResultRetryLogRepository;
    private final AgentApiCredentialService agentApiCredentialService;
    private final AuthenticationService authenticationService;
    private final RequestService requestService;
    private final HttpService httpService;

    public BetResultRetryLogService(RawBetResultRetryLogRepository rawBetResultRetryLogRepository,
                                    AgentApiCredentialService agentApiCredentialService,
                                    AuthenticationService authenticationService, RequestService requestService,
                                    HttpService httpService) {
        this.rawBetResultRetryLogRepository = rawBetResultRetryLogRepository;
        this.agentApiCredentialService = agentApiCredentialService;
        this.authenticationService = authenticationService;
        this.requestService = requestService;
        this.httpService = httpService;
    }

    public void create(String operatorData, Integer vendorId, Integer agentId, String betId, String roundId,
                       String internalTransactionId, String action) {

        RawBetResultRetryLog rawBetResultRetryLog = new RawBetResultRetryLog();
        Integer defaultRetryCounter = 1;
        Long nextRetryTime = System.currentTimeMillis();

        rawBetResultRetryLog.setId(internalTransactionId);
        rawBetResultRetryLog.setTransactionId(internalTransactionId);
        rawBetResultRetryLog.setAction(action);
        rawBetResultRetryLog.setVendorId(vendorId);
        rawBetResultRetryLog.setAgentId(agentId);
        rawBetResultRetryLog.setOperatorData(operatorData);
        rawBetResultRetryLog.setRetryCounter(defaultRetryCounter);
        rawBetResultRetryLog.setNextRetryTime(this.calculateNextRetryTime(defaultRetryCounter, nextRetryTime));
        rawBetResultRetryLog.setStatus(RetryStatus.FAILED.code);
        rawBetResultRetryLog.setCreateDate(nextRetryTime);
        rawBetResultRetryLog.setBetId(betId);
        rawBetResultRetryLog.setRoundId(roundId);

        rawBetResultRetryLogRepository.save(rawBetResultRetryLog);

    }

    private String getAction(String input) {
        int lastSlashIndex = input.lastIndexOf('/');

        if (lastSlashIndex == -1) {
            return input;
        }

        int secondLastSlashIndex = input.lastIndexOf('/', lastSlashIndex - 1);

        if (secondLastSlashIndex == -1) {
            return input;
        }

        return input.substring(secondLastSlashIndex);
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
            String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

            String traceId = UUID.randomUUID().toString();
            String updatedOperatorData = this.updateOperatorDataWithNewTraceId(operatorData, traceId);

            headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());
            httpRequestLog.setOperatorData(updatedOperatorData);
            httpRequestLog.setOperatorEndPoints(apiUrl + action);

            String signature = authenticationService.generateSignatureWithJson(updatedOperatorData, agentApiCredential.getApiSecret());
            headerMap.add(EndPoints.HEADER_SIGNATURE, signature);

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
                    .timeout(Duration.ofMillis(EndPoints.SPORTBOOK_TIMEOUT))
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

    public CompletableFuture<Void> processRetryRequest(RawBetResultRetryLog rawBetResultRetryLogItem, Long currentTime) {
        return CompletableFuture.runAsync(() -> {

            GeneralVo vo = new GeneralVo();
            HttpRequestLog httpRequestLog = httpService.startRetryRequestToOperator(rawBetResultRetryLogItem);
            httpRequestLog.setOperatorData(rawBetResultRetryLogItem.getOperatorData());
            httpRequestLog.setAgentId(rawBetResultRetryLogItem.getAgentId());
            httpRequestLog.setRoundId(rawBetResultRetryLogItem.getRoundId());

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                httpRequestLog.setRequestBody(objectMapper.writeValueAsString(rawBetResultRetryLogItem));

                rawBetResultRetryLogRepository.delete(rawBetResultRetryLogItem);
                this.call(rawBetResultRetryLogItem.getOperatorData(), rawBetResultRetryLogItem.getAction(), rawBetResultRetryLogItem.getAgentId(), httpRequestLog);
                rawBetResultRetryLogItem.setStatus(RetryStatus.SUCCESS.code);
                vo.setResponseCode(ResponseCode.SUCCESS);

            } catch (Exception e) {
                httpService.logError(httpRequestLog, e);
                vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);

                rawBetResultRetryLogItem.setStatus(RetryStatus.FAILED.code);
                rawBetResultRetryLogItem.setRetryCounter(rawBetResultRetryLogItem.getRetryCounter() + 1);
                rawBetResultRetryLogItem.setNextRetryTime(this.calculateNextRetryTime(rawBetResultRetryLogItem.getRetryCounter(), currentTime));

                if (rawBetResultRetryLogItem.getRetryCounter().equals(maxRetryCounter)) {
                    rawBetResultRetryLogItem.setStatus(RetryStatus.TIMEOUT.code);
                } else {
                    //will only save back once is failed, and not hitting maxRetryCounter
                    rawBetResultRetryLogRepository.save(rawBetResultRetryLogItem);
                }

            } finally {
                httpService.end(httpRequestLog, vo);
            }
        });
    }

    public CompletableFuture<Void> asyncProcessRetryRequestByList(List<RawBetResultRetryLog> rawBetResultRetryLogList, Long currentTime) {
        List<CompletableFuture<Void>> futures = rawBetResultRetryLogList.stream()
                .map(rawBetResultRetryLogListItem -> processRetryRequest(rawBetResultRetryLogListItem, currentTime))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public WalletBalanceVo processForceSuccess(String traceId, String agentPlayerUsername, String currencyCode, BetInformation betInformation) {

        WalletBalanceVo responseVo = new WalletBalanceVo();
        WalletBalanceVo.ResponseData data = new WalletBalanceVo.ResponseData();
        BigDecimal balance = (betInformation.getBalance() == null) ? BigDecimal.ZERO : betInformation.getBalance();

        data.setBalance(balance);
        data.setUsername(agentPlayerUsername);
        data.setCurrency(currencyCode);
        data.setTimestamp(System.currentTimeMillis());

        responseVo.setTraceId(traceId);
        responseVo.setStatus(ResponseCodes.Status.SC_OK);
        responseVo.setMessage(ResponseCodes.Status.SC_OK.description);
        responseVo.setData(data);

        return responseVo;
    }

}
