package com.nextgen.gameaggregator.vendor.pinnacle.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {
    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private GameSessionService gameSessionService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public GameUrlService() {
        super(GameUrlVo.class);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String oddsFormat = Optional.ofNullable(credentials.get(Credentials.ODDS_FORMAT)).orElseThrow(InvalidVendorLineException::new);
        formData.add("locale", gameSession.getVendorLanguageCode());
        formData.add("oddsFormat", oddsFormat);
        formData.add("loginId", gameSession.getVendorPlayerUsername());

        return formData;
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorLineException, InvalidVendorResponseException, InvalidTimeoutException {
        GameUrlVo responseVo = null;

        responseVo = this.loginV2(formData, credentials, gameSession);

        return responseVo;
    }

    public GameUrlVo loginV2(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException, InvalidTimeoutException {

        String apiUrl = Optional.ofNullable(credentials.get(Credentials.API_URL)).orElseThrow(InvalidVendorLineException::new);
        String agentCode = Optional.ofNullable(credentials.get(Credentials.AGENT_CODE)).orElseThrow(InvalidVendorLineException::new);
        String agentKey = Optional.ofNullable(credentials.get(Credentials.AGENT_KEY)).orElseThrow(InvalidVendorLineException::new);
        String secretKey = Optional.ofNullable(credentials.get(Credentials.SECRET_KEY)).orElseThrow(InvalidVendorLineException::new);

        GameUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add("userCode", agentCode);
        headerMap.add("token", VendorService.generateToken(agentCode, agentKey, secretKey));

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.PLAYER_LOGIN_V2)
                .headers(header -> header.addAll(headerMap))
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(Endpoints.RETRY)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.PLAYER_LOGIN_V2, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(Objects.requireNonNull(apiResponse));
            responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

            // 2. validate vendor response
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }
        return responseVo;
    }
}
