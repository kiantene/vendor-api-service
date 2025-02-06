package com.nextgen.gameaggregator.vendor.bglive.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


public class GameUrlService extends BaseGameUrlService<BgLiveGameUrlVo> {

    private String apiUrl1;
    private String apiUrl2;
    private String snCode;
    private String agentKey;
    private String agentPass;
    private String secretCode;

    public GameUrlService() {

        super(BgLiveGameUrlVo.class);
        this.setAutoMapResponse(false);
        this.setContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        this.agentKey = ValidationUtils.validateCredential(credentials.get(Credentials.AGENT_KEY));
        this.agentPass = ValidationUtils.validateCredential(credentials.get(Credentials.AGENT_PASS));
        this.apiUrl1 = ValidationUtils.validateCredential(credentials.get(Credentials.API_URL1));
        this.apiUrl2 = ValidationUtils.validateCredential(credentials.get(Credentials.API_URL2));
        this.snCode = ValidationUtils.validateCredential(credentials.get(Credentials.SN_CODE));
        this.secretCode = VendorService.generateSecretCode(agentPass);

        return new LinkedMultiValueMap<>();
    }

    @Override
    public BgLiveGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        try {
            this.createAccount(gameSession, httpRequestLog);
        } catch (Exception e) {
            throw new InvalidVendorResponseException("Failed to checkAndCreateAccount or getBalance or createSessionToken : " + e);
        }
        String uuid = UUID.randomUUID().toString();
        String digest;
        try {
            digest = VendorService.encryptLoginMd5Key(uuid, snCode, gameSession.getVendorPlayerUsername(), secretCode);
        } catch (InvalidFormatException e) {
            throw new InvalidVendorResponseException("MD5 Encryption Failed" + e);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("random", uuid);
        params.put("sn", snCode);
        params.put("loginId", gameSession.getVendorPlayerUsername());
        params.put("digest", digest);

        Map<String, Object> formLoginData = new HashMap<String, Object>();
        formLoginData.put("id", uuid);
        formLoginData.put("method", EndPoints.GAME_URL);
        formLoginData.put("params", params);
        formLoginData.put("jsonrpc", "2.0");
        httpRequestLog.setUrl(apiUrl1);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        WebClient webClient = WebClient.create();
        ResponseEntity<String> response = webClient.post()
                .uri(apiUrl1 + EndPoints.GAME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(formLoginData)
                .retrieve()
                .toEntity(String.class)
                .retry(RETRY_COUNT)
                .timeout(Duration.ofMillis(TIMEOUT))
                .onErrorResume(TimeoutException.class, e -> {
                    isTimeout.set(true);
                    return Mono.error(e);
                })
                .block();


        this.validateResponse(response, isTimeout, httpRequestLog, BgLiveGameUrlVo.class, gameSession);
        if (response == null || response.getBody() == null) {
            throw new InvalidVendorResponseException("MD5 Encryption Failed");
        }
        try {
            String body = response.getBody();
            LoginDto loginDto = HttpService.convertJsonToDto(body, LoginDto.class);
            String gameUrl = loginDto.getResult();
            BgLiveGameUrlVo responseVo = new BgLiveGameUrlVo();
            responseVo.setData(gameUrl);

            return responseVo;
        } catch (Exception e) {
            throw new RuntimeException("Error processing JSON", e);
        }

    }


    private void createAccount(GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException, JsonProcessingException, InvalidFormatException {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> params = new HashMap<>();
        String uuid = UUID.randomUUID().toString();
        String digest = VendorService.encryptCreateUserMd5Key(uuid, snCode, secretCode);
        params.put("random", uuid);
        params.put("sn", snCode);
        params.put("loginId", gameSession.getVendorPlayerUsername());
        params.put("agentLoginId", agentKey);
        params.put("digest", digest);

        Map<String, Object> formData = new HashMap<String, Object>();
        formData.put("id", uuid);
        formData.put("method", EndPoints.CREATE_USER);
        formData.put("params", params);
        formData.put("jsonrpc", "2.0");


        httpRequestLog.setUrl(this.apiUrl1 + EndPoints.CREATE_USER);
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        WebClient webClient = WebClient.create();
        ResponseEntity<String> response = webClient.post()
                .uri(apiUrl1 + EndPoints.CREATE_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(formData)
                .retrieve()
                .toEntity(String.class)
                .retry(RETRY_COUNT)
                .timeout(Duration.ofMillis(TIMEOUT))
                .onErrorResume(TimeoutException.class, e -> {
                    isTimeout.set(true);
                    return Mono.error(e);
                })
                .block();

        this.validateResponse(response, isTimeout, httpRequestLog, BgLiveGameUrlVo.class, gameSession);

        BgLiveGameUrlVo responseVo = objectMapper.readValue(response.getBody(), BgLiveGameUrlVo.class);

        if (responseVo.isSuccess()) {
            return;
        }
        if (responseVo.getError() != null && "2206".equals(responseVo.getError().getCode())) {
            return;
        }
        throw new InvalidVendorResponseException("Failed to Create Account : " + response.getBody());
    }

}
