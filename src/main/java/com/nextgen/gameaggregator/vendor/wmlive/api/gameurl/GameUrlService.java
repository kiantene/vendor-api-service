package com.nextgen.gameaggregator.vendor.wmlive.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.wmlive.constant.Credentials;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.nextgen.gameaggregator.vendor.wmlive.constant.Commands.LOGIN;
import static com.nextgen.gameaggregator.vendor.wmlive.constant.Commands.REGISTER;

@Service
@Slf4j
@Getter
public class GameUrlService extends BaseGameUrlService<WMGameUrlVo> {

    String launchUrl;
    String vendorId;
    String signature;
    String user;
    String password;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public GameUrlService() {
        super(WMGameUrlVo.class);
        this.setAutoMapResponse(false);
        this.setHttpMethod(HttpMethod.POST);
        this.setCredentialApiUrl(Credentials.LAUNCH_URL);

    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        this.launchUrl = ValidationUtils.validateCredential(credentials.get(Credentials.LAUNCH_URL));
        this.vendorId = ValidationUtils.validateCredential(credentials.get(Credentials.VENDOR_ID));
        this.signature = ValidationUtils.validateCredential(credentials.get(Credentials.SIGNATURE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("cmd", LOGIN);
        formData.add("vendorId", this.vendorId);
        formData.add("signature", this.signature);
        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("password", gameSession.getVendorPlayerUsername());
        formData.add("lang", gameSession.getLanguage()); // or other language code
        formData.add("isTest", "0"); // or other test value
        formData.add("timestamp", "0"); // or other timestamp value
        formData.add("syslang", gameSession.getLanguage()); // or other system language value

        return formData;
    }

    @Override
    public WMGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                    GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        try {
            this.checkAndCreateAccount(gameSession);
        } catch (Exception e) {
            throw new InvalidVendorResponseException(e.getMessage());
        }
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        HttpHeaders httpHeaders = this.getHeaders(new HttpHeaders(), formData, credentials, gameSession);

        URI urlScheme = UriComponentsBuilder.fromUriString(this.getLaunchUrl())
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        // Trigger doPost to get game url function by calling vendor api
        ResponseEntity<String> response = this.doPost(this.getLaunchUrl(), urlScheme.toString(), httpHeaders, formData, isTimeout);

        this.validateResponse(response, isTimeout, httpRequestLog, WMGameUrlVo.class, gameSession);

        WMGameUrlVo responseVo = new Gson().fromJson(response.getBody(), WMGameUrlVo.class);

        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }

    private void checkAndCreateAccount(GameSession gameSession) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("cmd", REGISTER);
        formData.add("vendorId", vendorId);
        formData.add("signature", signature);
        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("password", gameSession.getVendorPlayerUsername());
        formData.add("username", gameSession.getVendorPlayerUsername());
        formData.add("timestamp", "0"); // or other timestamp value
        formData.add("syslang", gameSession.getLanguage()); // or other system language value

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        this.doGet(this.getLaunchUrl(), "", formData, isTimeout);

    }

}
