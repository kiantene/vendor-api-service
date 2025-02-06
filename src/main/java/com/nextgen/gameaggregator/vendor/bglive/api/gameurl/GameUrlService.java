package com.nextgen.gameaggregator.vendor.bglive.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


public class GameUrlService extends BaseGameUrlService<BgLiveGameUrlVo> {

    private String apiUrl1;
    private String apiUrl2;
    private String apiKey;
    private String snCode;
    private String agentKey;
    private String agentPass;
    private String secretCode;
    private HttpHeaders httpHeaders;

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
        this.apiKey = ValidationUtils.validateCredential(credentials.get(Credentials.API_KEY));
        this.snCode = ValidationUtils.validateCredential(credentials.get(Credentials.SN_CODE));
        this.secretCode = VendorService.generateSecretCode(agentPass);

        String uuid = UUID.randomUUID().toString();
        String digest = VendorService.encryptMd5Key(uuid, snCode, secretCode);

        // set DTO
        UserDto userDto = VendorService.setUserDto(uuid, digest, snCode, gameSession.getVendorPlayerUsername(), agentKey);


        Map<String, Object> body = new LinkedHashMap<>();
        body.put("params", userDto);
        body.put("id", "123");
        body.put("method", EndPoints.GAME_URL);
        body.put("jsonrpc", "2.0");

        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("body", new Gson().toJson(body));
        return formData;

//        MultiValueMap<String, String> formDataJson = null;
//        try {
//            formDataJson = buildFormDataJson(gameSession);
//        } catch (JsonProcessingException e) {
//            throw new InvalidFormatException("Failed to process JSON");
//        }
//        return formDataJson;
    }

//    private MultiValueMap<String, String> buildFormDataJson(GameSession gameSession) throws JsonProcessingException {
//        MultiValueMap<String, String> formDataJson = new LinkedMultiValueMap<>();
//        formDataJson.add("id", "123");
//        formDataJson.add("method", EndPoints.GAME_URL);
//        formDataJson.add("jsonrpc", "2.0");
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("random", uuid);
//        params.put("digest", digest);
//        params.put("sn", snCode);
//        params.put("loginId", gameSession.getVendorPlayerUsername());
//
//        String paramsJson = new ObjectMapper().writeValueAsString(params);
//        formDataJson.add("params", paramsJson);
//        return formDataJson;
//    }

    @Override
    public BgLiveGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        try {
            this.createAccount(gameSession, httpRequestLog);
        } catch (Exception e) {
            throw new InvalidVendorResponseException("Failed to checkAndCreateAccount or getBalance or createSessionToken : " + e);
        }

        httpRequestLog.setUrl(apiUrl1);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        URI uri = UriComponentsBuilder.fromUriString(apiUrl1)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> response = ResponseEntity.ok().body(uri.toString());

        this.validateResponse(response, isTimeout, httpRequestLog, BgLiveGameUrlVo.class, gameSession);

        BgLiveGameUrlVo responseVo = new BgLiveGameUrlVo();
        responseVo.setData(uri.toString());

        return responseVo;
    }


    private void createAccount(GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException, JsonProcessingException, InvalidFormatException {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> params = new HashMap<>();
        String uuid = UUID.randomUUID().toString();
        String digest = VendorService.encryptMd5Key(uuid, snCode, secretCode);
        params.put("random", uuid);
        params.put("sn", snCode);
        params.put("loginId", gameSession.getVendorPlayerUsername());
        params.put("agentLoginId", agentKey);
        params.put("digest", digest);

        Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("id", uuid);
        postData.put("method", EndPoints.CREATE_USER);
        postData.put("params", params);
        postData.put("jsonrpc", "2.0");


        httpRequestLog.setUrl(this.apiUrl1 + EndPoints.CREATE_USER);
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        HttpHeaders headers = new HttpHeaders();
//
        WebClient webClient = WebClient.create();
        ResponseEntity<String> response = webClient.post()
                .uri(apiUrl1 + EndPoints.CREATE_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(postData)
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
