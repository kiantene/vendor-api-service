package com.nextgen.gameaggregator.vendor.spinix.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.spinix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {

        Map<String, Object> arrayMap = new HashMap<>();
        arrayMap.put("platform_id", credentials.get(Credentials.AGENT_ID));
        arrayMap.put("game_id", gameCode);
        arrayMap.put("user_id", gameSession.getVendorPlayerUsername());
        arrayMap.put("user_token", gameSession.getToken());
        arrayMap.put("currency", gameSession.getCurrencyCode());
        arrayMap.put("wallet_type", credentials.get(Credentials.WALLET_TYPE));
        HashMap<String, String> settings = new HashMap<>();
        settings.put("lang", gameSession.getLanguage());
        settings.put("sd", credentials.get(Credentials.SOUND));
        arrayMap.put("settings", settings);
        String json = new Gson().toJson(arrayMap);

        VendorService vendorService = new VendorService();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("json", json);
        formData.add("x_gaming_signature", vendorService.getSignature(arrayMap, credentials.get(Credentials.SIGNATURE_KEY)));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info(formData.getFirst("json"));
        log.info(formData.getFirst("x_gaming_signature"));

        // TODO: need to add error handling
        GameUrlVendorResponseVo responseVo = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(formData.getFirst("json")))
                .header("Authorization", credentials.get(Credentials.SECRET_KEY))
                .header("X-Gaming-Signature", formData.getFirst("x_gaming_signature"))
                .retrieve()
                .onStatus(HttpStatus::isError,
                        response -> {
                            HttpStatus clientResponseStatus = response.statusCode();
                            return response.bodyToMono(String.class).map(body ->
                                    new InvalidVendorResponseException
                                            ("response status :" + clientResponseStatus + ", response body :" + body));
                        })
                .bodyToMono(GameUrlVendorResponseVo.class)
                .block();

        if (responseVo.getData() != null) {
            log.info(responseVo.toString());
        } else {
            throw new InvalidVendorResponseException("Invalid Response : " + responseVo.toString());
        }

        return responseVo.getData();
    }
}
