package com.nextgen.gameaggregator.vendor.spinix.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.spinix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String agent_id = credentials.get(Credentials.AGENT_ID);
        Optional.ofNullable(agent_id).orElseThrow(InvalidVendorLineException::new);
        String wallet_type = credentials.get(Credentials.WALLET_TYPE);
        Optional.ofNullable(wallet_type).orElseThrow(InvalidVendorLineException::new);
        String sound = credentials.get(Credentials.SOUND);
        Optional.ofNullable(sound).orElseThrow(InvalidVendorLineException::new);
        String signature_key = credentials.get(Credentials.SIGNATURE_KEY);
        Optional.ofNullable(signature_key).orElseThrow(InvalidVendorLineException::new);

        Map<String, Object> arrayMap = new HashMap<>();
        arrayMap.put("platform_id", agent_id);
        arrayMap.put("game_id", gameSession.getVendorGameCode());
        arrayMap.put("user_id", gameSession.getVendorPlayerUsername());
        arrayMap.put("user_token", gameSession.getToken());
        arrayMap.put("currency", gameSession.getVendorCurrencyCode());
        arrayMap.put("wallet_type", wallet_type);
        HashMap<String, String> settings = new HashMap<>();
        settings.put("lang", gameSession.getLanguage());
        settings.put("sd", sound);
        arrayMap.put("settings", settings);
        String json = new Gson().toJson(arrayMap);

        VendorService vendorService = new VendorService();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("json", json);
        formData.add("x_gaming_signature", vendorService.getSignature(arrayMap, signature_key));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        String secretKey = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info("Spinix GameUrlService: " + formData.getFirst("json").toString());

        GameUrlVendorResponseVo responseVo = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(formData.getFirst("json")))
                .header("Authorization", secretKey)
                .header("X-Gaming-Signature", formData.getFirst("x_gaming_signature"))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> Mono.empty())
//                .onStatus(HttpStatus::isError,
//                        response -> {
//                            HttpStatus clientResponseStatus = response.statusCode();
//                            return response.bodyToMono(String.class).map(body ->
//                                    new InvalidVendorResponseException
//                                            ("response status :" + clientResponseStatus + ", response body :" + body));
//                        })
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
