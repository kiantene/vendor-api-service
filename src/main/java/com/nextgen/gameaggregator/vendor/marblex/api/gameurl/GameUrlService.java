package com.nextgen.gameaggregator.vendor.marblex.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.HmacUtils;
import com.nextgen.gameaggregator.util.MapUtils;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.constant.Credentials;
import com.nextgen.gameaggregator.vendor.marblex.constant.EndPoints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;

public class GameUrlService extends BaseGameUrlService<VendorGameUrlVo> {
    // credential value
    private String apiKey;
    private String apiSecret;
    private Integer deviceType;

    public GameUrlService() {
        super(VendorGameUrlVo.class);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        this.apiKey = ValidationUtils.validateCredential(credentials.get(Credentials.API_KEY));
        this.apiSecret = ValidationUtils.validateCredential(credentials.get(Credentials.API_SECRET));
        this.deviceType = Integer.valueOf(gameSession.getVendorPlatformCode());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("TraceID", gameSession.getTraceId());
        formData.add("PlayerID", gameSession.getVendorPlayerUsername());
        formData.add("Currency", gameSession.getVendorCurrencyCode());
        formData.add("Language", gameSession.getVendorLanguageCode());
        formData.add("GameCode", gameSession.getVendorGameCode()+"-QAT"); //temporary add "-QAT"
        formData.add("MerchantID", "");
        return formData;
    }

    @Override
    protected HttpHeaders getHeaders(HttpHeaders httpHeaders, MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) {
        Map<String, Object> normalizedMap = MapUtils.convertMultiValueMapToMap(formData);
        normalizedMap.put("DeviceType", this.deviceType);
        normalizedMap.put("DemoAccount", false);
        String signature = HmacUtils.generate("HmacSHA256",this.apiSecret, new Gson().toJson(normalizedMap));

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add("Authorization", "JanusV1 " + this.apiKey + ',' + signature);
        return new HttpHeaders(headerMap);
    }

    @Override
    protected BodyInserter<?, ? super ClientHttpRequest> getBody(MultiValueMap<String, String> formData) {
        Map<String, Object> normalizedMap = MapUtils.convertMultiValueMapToMap(formData);
        normalizedMap.put("DeviceType", this.deviceType);
        normalizedMap.put("DemoAccount", false);
        return BodyInserters.fromValue(normalizedMap);
    }

}
