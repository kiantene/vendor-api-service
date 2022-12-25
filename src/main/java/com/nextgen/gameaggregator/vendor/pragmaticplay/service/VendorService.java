package com.nextgen.gameaggregator.vendor.pragmaticplay.service;

import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
//import com.nextgen.gameaggregator.operator.game.url.GameUrlData;
//import com.nextgen.gameaggregator.operator.game.url.VendorGameUrlVo;
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
public class VendorService {
//    public GameUrlData getGameUrl(String gameCode, String language, String token, Map<String, String> credentials) throws InvalidVendorLineException {
//        String endpoint = Endpoints.GAME_URL;
//        String apiUrl = credentials.get(Credentials.API_URL);
//        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
//
//        String secureLogin = credentials.get(Credentials.SECURE_LOGIN);
//        Optional.ofNullable(secureLogin).orElseThrow(InvalidVendorLineException::new);
//
//        String secret = credentials.get(Credentials.SECRET_KEY);
//        Optional.ofNullable(secret).orElseThrow(InvalidVendorLineException::new);
//
//        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
//        paramMap.add("secureLogin", secureLogin);
//        paramMap.add("symbol", gameCode);
//        paramMap.add("language", language);
//        paramMap.add("token", token);
//        String hash = generateHash(paramMap, secret);
//        paramMap.add("hash", hash);
//
//        WebClient webClient = WebClient.create(apiUrl);
//        VendorGameUrlVo result = webClient.post()
//                .uri(endpoint)
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .body(BodyInserters.fromFormData(paramMap))
//                .retrieve()
//                .bodyToMono(VendorGameUrlVo.class)
//                .block();
//
//        GameUrlData gameUrlData = new GameUrlData();
//        if (result != null) { // TODO: need to check error
//            gameUrlData.setGameUrl(result.getGameURL());
//            gameUrlData.setToken(token);
//        }
//        return gameUrlData;
//    }

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

    public static void validateHash(String hash, String secretKey, String requestBody) throws InvalidSignatureException {
        String requestData = requestBody.replaceAll("(^|&)hash=.*?(&|$)", "$1$2");
        String generatedHash = generateHash(requestData, secretKey);
        log.info("Request data: " + requestData);
        log.info("Generated hash: " + generatedHash);
        if (!hash.equals(generatedHash)) {
            throw new InvalidSignatureException();
        }
    }
}
