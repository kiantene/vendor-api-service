package com.nextgen.gameaggregator.vendor.poker365.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.poker365.constant.EndPoints;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@Slf4j
@Getter
@Setter
public class GameUrlService extends BaseGameUrlService<Poker365GameUrlVo> {

    HttpHeaders headers = new HttpHeaders();
    private String cert;
    private String account;
    private String key;
    private String apiUrl;
    private String launchGameUrl;
    private String website;
    private HttpHeaders httpHeaders;
    private String keyUrl;
    private String api = "/api/";

    public GameUrlService() {

        super(Poker365GameUrlVo.class);
        this.setAutoMapResponse(false);
        this.setGameUrl(EndPoints.LAUNCH_GAME);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException,
            InvalidFormatException {


        this.apiUrl = ValidationUtils.validateCredential(credentials.get(Credentials.API_URL));
        this.cert = ValidationUtils.validateCredential(credentials.get(Credentials.CERT));
        this.account = ValidationUtils.validateCredential(credentials.get(Credentials.ACCOUNT));
        this.website = ValidationUtils.validateCredential(credentials.get(Credentials.WEBSITE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", String.valueOf(gameSession.getVendorPlayerId()));
        formData.add("extension1", account);
        formData.add("userName", gameSession.getVendorPlayerUsername());
        formData.add("gameId", gameSession.getVendorGameCode());
        //need discuss
        formData.add("jackpot", "1");

        return formData;


    }

    @Override
    public Poker365GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                          GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException,
            TimeoutException,
            InvalidVendorLineException {

        try {
            key = this.getKey(gameSession, httpRequestLog);
            formData.add("key", key);
        } catch (Exception e) {
            throw new InvalidVendorResponseException("Failed to get Key: " + e);
        }
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        httpRequestLog.setUrl(this.getApiUrl() + api + website + EndPoints.LAUNCH_GAME);
        this.launchGameUrl = this.getApiUrl() + api + website;
        ResponseEntity<String> response = this.doPost(this.getLaunchGameUrl(), EndPoints.LAUNCH_GAME, headers, formData, isTimeout);
        this.validateResponse(response, isTimeout, httpRequestLog, Poker365GameUrlVo.class, gameSession);

        return new Gson().fromJson(response.getBody(), Poker365GameUrlVo.class);
    }


    private String getKey(GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException, JsonProcessingException, InvalidFormatException {

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("cert", cert);
        param.add("user", String.valueOf(gameSession.getVendorPlayerId()));
        param.add("userName", gameSession.getVendorPlayerUsername());
        param.add("extension1", account);
        param.add("currency", gameSession.getVendorCurrencyCode());


        httpRequestLog.setUrl(this.getApiUrl() + api + website + EndPoints.KEY);
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        this.keyUrl = this.getApiUrl() + api + website;

        ResponseEntity<String> response = this.doPost(this.getKeyUrl(), EndPoints.KEY, headers, param, isTimeout);
        this.validateResponse(response, isTimeout, httpRequestLog, Poker365GameUrlVo.class, gameSession);
        try {
            Map<String, Object> responseData = new Gson().fromJson(
                    response.getBody(),
                    new TypeToken<Map<String, Object>>() {
                    }.getType()
            );
            if (responseData == null || !responseData.containsKey("key")) {
                throw new InvalidVendorResponseException("Response does not contain a valid 'key'.");
            }
            return responseData.get("key").toString();

        } catch (Exception e) {
            throw new InvalidVendorResponseException();
        }

    }
}
