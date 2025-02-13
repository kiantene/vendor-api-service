package com.nextgen.gameaggregator.vendor.ag.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ag.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ag.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ag.service.VendorService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@Slf4j
@Getter
@Setter
public class GameUrlService extends BaseGameUrlService<AgGameUrlVo> {

    private static final String ACTYPE = "1";
    private static final String METHOD = "lg";

    @Autowired
    private WalletService walletService;

    // credential value
    private String md5Key;
    private String desKey;
    private String cAgent;
    private String productId;
    private String checkAccountUrl;
    private String createSessionUrl;
    private String launchGameUrl;
    // XML Mapper
    private XmlMapper xmlMapper;


    public GameUrlService() {

        super(AgGameUrlVo.class);
        this.setAutoMapResponse(false);
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        this.md5Key = ValidationUtils.validateCredential(credentials.get(Credentials.MD5KEY));
        this.desKey = ValidationUtils.validateCredential(credentials.get(Credentials.DESKEY));
        this.cAgent = ValidationUtils.validateCredential(credentials.get(Credentials.CAGENT));
        this.productId = ValidationUtils.validateCredential(credentials.get(Credentials.ACCOUNT));
        this.checkAccountUrl = ValidationUtils.validateCredential(credentials.get(Credentials.GI_URL));
        this.createSessionUrl = ValidationUtils.validateCredential(credentials.get(Credentials.CREATE_SESSION_KEY));
        this.launchGameUrl = ValidationUtils.validateCredential(credentials.get(Credentials.GCI_URL));

        String sid = VendorService.generateRandomNumber(this.getCAgent());

        Map<String, String> formDataJson = buildFormDataJson(gameSession, sid);

        String paramString = VendorService.buildParamString(formDataJson);
        String encryptParams = VendorService.encrypt(paramString, this.getDesKey());
        String encryptKey = VendorService.encryptMd5Key(encryptParams, this.getMd5Key());

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("params", encryptParams);
        param.add("key", encryptKey);

        return param;
    }

    private Map<String, String> buildFormDataJson(GameSession gameSession, String sid) {
        Map<String, String> formDataJson = new LinkedHashMap<>();
        formDataJson.put("cagent", this.getCAgent());
        formDataJson.put("loginname", gameSession.getVendorPlayerUsername());
        formDataJson.put("actype", ACTYPE);
        formDataJson.put("password", gameSession.getVendorPlayerUsername());
        formDataJson.put("sid", sid);
        formDataJson.put("lang", gameSession.getVendorLanguageCode());
        formDataJson.put("gameType", gameSession.getVendorGameCode());
        formDataJson.put("cur", gameSession.getVendorCurrencyCode());
        return formDataJson;
    }

    @Override
    public AgGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        try {
            this.checkAndCreateAccount(gameSession, httpRequestLog);
            String credit = getBalance(gameSession, httpRequestLog);
            this.createSessionToken(credit, gameSession, httpRequestLog);
        } catch (Exception e) {
            throw new InvalidVendorResponseException("Failed to checkAndCreateAccount or getBalance or createSessionToken : " + e);
        }

        httpRequestLog.setUrl(this.getLaunchGameUrl() + EndPoints.LAUNCH_GAME);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        URI uri = UriComponentsBuilder.fromUriString(this.getLaunchGameUrl())
                .path(EndPoints.LAUNCH_GAME)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> response = ResponseEntity.ok().body(uri.toString());

        this.validateResponse(response, isTimeout, httpRequestLog, AgGameUrlVo.class, gameSession);

        AgGameUrlVo responseVo = new AgGameUrlVo();
        responseVo.setData(uri.toString());

        return responseVo;
    }

    private void checkAndCreateAccount(GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException, JsonProcessingException, InvalidFormatException {

        Map<String, String> formDataJson = new LinkedHashMap<>();
        formDataJson.put("cagent", this.getCAgent());
        formDataJson.put("loginname", gameSession.getVendorPlayerUsername());
        formDataJson.put("method", METHOD);
        formDataJson.put("actype", ACTYPE);
        formDataJson.put("password", gameSession.getVendorPlayerUsername());
        formDataJson.put("cur", gameSession.getVendorCurrencyCode());

        String paramString = VendorService.buildParamString(formDataJson);
        String encryptParams = VendorService.encrypt(paramString, this.getDesKey());
        String encryptKey = VendorService.encryptMd5Key(encryptParams, this.getMd5Key());

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("params", encryptParams);
        param.add("key", encryptKey);

        httpRequestLog.setUrl(this.getCheckAccountUrl() + EndPoints.CHECKANDCREATE_ACCOUNT);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        ResponseEntity<String> response = this.doGet(this.getCheckAccountUrl(), EndPoints.CHECKANDCREATE_ACCOUNT, param, isTimeout);
        this.validateResponse(response, isTimeout, httpRequestLog, AgGameUrlVo.class, gameSession);

        AgGameUrlVo responseVo = xmlMapper.readValue(response.getBody(), AgGameUrlVo.class);
        if (!responseVo.getInfoCheckAndCreate().equalsIgnoreCase("0")) {
            throw new InvalidVendorResponseException("Failed to checkAndCreateAccount : " + response.getBody());
        }
    }

    private void createSessionToken(String credit, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException, JsonProcessingException {

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("productid", this.getProductId());
        param.add("username", gameSession.getVendorPlayerUsername());
        param.add("session_token", gameSession.getToken());
        param.add("credit", credit);

        httpRequestLog.setUrl(this.getCreateSessionUrl() + EndPoints.SESSION_TOKEN);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        ResponseEntity<String> response = this.doGet(this.getCreateSessionUrl(), EndPoints.SESSION_TOKEN, param, isTimeout);
        this.validateResponse(response, isTimeout, httpRequestLog, AgGameUrlVo.class, gameSession);

        AgGameUrlVo responseVo = xmlMapper.readValue(response.getBody(), AgGameUrlVo.class);
        if (!responseVo.getInfoSessionToken().equalsIgnoreCase("OK")) {
            throw new InvalidVendorResponseException("Failed to createSessionToken : " + response.getBody());
        }
    }

    public String getBalance(GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorLineException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        String traceId = httpRequestLog.getId();
        String credit;

        credit = walletService.getBalance(traceId, gameSession, httpRequestLog).toString();

        if (credit == null || new BigDecimal(credit).compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidVendorLineException("Balance cannot be null or negative: " + credit);
        }
        return credit;

    }
}





