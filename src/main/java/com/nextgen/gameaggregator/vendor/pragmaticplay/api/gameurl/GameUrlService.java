package com.nextgen.gameaggregator.vendor.pragmaticplay.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        String secureLogin = credentials.get(Credentials.SECURE_LOGIN);
        Optional.ofNullable(secureLogin).orElseThrow(InvalidVendorLineException::new);

        String secret = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secret).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secureLogin", secureLogin);
        formData.add("symbol", gameSession.getVendorGameCode());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("technology", "H5");
        formData.add("token", gameSession.getToken());
        formData.add("platform", gameSession.getVendorPlatformCode());
        formData.add("currency", gameSession.getVendorCurrencyCode());
        String hash = VendorService.generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + Endpoints.GAME_URL);
        log.info(formData.toString());

        // TODO: need to add error handling
        String responseString =  WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.GAME_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
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
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        GameUrlVo responseVo = null;
        try {
            responseVo = new Gson().fromJson(responseString, GameUrlVo.class);
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new InvalidVendorResponseException( "Invalid vendor response body :"+responseString);
        }

        if (responseVo != null) {
            log.info(responseVo.toString());
        }

        return responseVo;
    }
}
