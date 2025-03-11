package com.nextgen.gameaggregator.vendor.kypoker.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.kypoker.constant.Actions;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@Getter
@Setter
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    @Autowired
    private WalletService walletService;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setHttpMethod(HttpMethod.GET);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String TimeStamp = String.valueOf(System.currentTimeMillis());
        String AgentId = (ValidationUtils.validateCredential(credentials.get(Credentials.AGENT_ID)));
        String AesKey = (ValidationUtils.validateCredential(credentials.get(Credentials.AES_KEY)));
        String Md5Key = (ValidationUtils.validateCredential(credentials.get(Credentials.MD5_KEY)));
        String aesString;

        MultiValueMap<String, String> encryptParam = new LinkedMultiValueMap<>();
        encryptParam.add("s", Actions.LOGIN);
        encryptParam.add("account", gameSession.getVendorPlayerUsername());
        encryptParam.add("orderid", AgentId + TimeStamp + gameSession.getVendorPlayerUsername());
        encryptParam.add("ip", gameSession.getIpAddress());
        encryptParam.add("lineCode", String.valueOf(gameSession.getVendorLineId()));
        encryptParam.add("KindID", gameSession.getVendorGameCode());
        encryptParam.add("money", "0");
        encryptParam.add("currency", String.valueOf(gameSession.getVendorCurrencyCode()));

        String queryString = UriComponentsBuilder.fromPath("")
                .queryParams(encryptParam)
                .build()
                .encode()
                .toString()
                .replaceFirst("^\\?", ""); // Remove leading "?"

        String encodedParam;
        try {
            aesString = VendorService.aesEncrypt(queryString, AesKey);
            encodedParam =  URLEncoder.encode(aesString, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("agent", AgentId);
        param.add("timestamp", TimeStamp);
        param.add("param", encodedParam);
        param.add("key", VendorService.MD5Encrypt(AgentId+TimeStamp+Md5Key));


        return param;
    }
    @Override
    protected ResponseEntity<String> doGet(String baseUrl, String uri, MultiValueMap<String, String> formData, AtomicBoolean isTimeout) {

        URI getUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(uri)
                .queryParams(formData)
                .build(true)  // 确保 queryParams 不会再次编码
                .toUri();

        return WebClient.create().get().uri(getUri)
                .retrieve()
                .toEntity(String.class)
                .retry(RETRY_COUNT)
                .timeout(Duration.ofMillis(TIMEOUT))
                .onErrorResume(TimeoutException.class, e -> {
                    isTimeout.set(true);
                    return Mono.error(e);
                })
                .block();
    }

}





