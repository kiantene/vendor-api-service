package com.nextgen.gameaggregator.vendor.pragmaticplay.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameUrlAction implements GameUrl {

    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {
        String secureLogin = credentials.get(Credentials.SECURE_LOGIN);
        Optional.ofNullable(secureLogin).orElseThrow(InvalidVendorLineException::new);

        String secret = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secret).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secureLogin", secureLogin);
        formData.add("symbol", gameCode);
        formData.add("language", gameSession.getLanguage());
        formData.add("token", gameSession.getToken());
        String hash = generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }

    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + Endpoints.GAME_URL);
        log.info(formData.toString());

        // TODO: need to add error handling
        GameUrlVo responseVo = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.GAME_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(GameUrlVo.class)
                .block();

        if (responseVo != null) {
            log.info(responseVo.toString());
        }

        return responseVo;
    }

    public static String generateHash(MultiValueMap<String, String> params, String secret) {
        String payload = params.keySet().stream().sorted()
                .map(key -> key + "=" + params.get(key).get(0))
                .collect(Collectors.joining("&"));

        return generateHash(payload, secret);
    }

    public static String generateHash(String payload, String secret) {
        payload += secret;
        return DigestUtils.md5Hex(payload);
    }
}
