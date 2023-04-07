package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("account", gameSession.getVendorPlayerUsername());
        formData.add("gamehall", "CQ9");
        formData.add("gamecode", gameCode);
        formData.add("gameplat", gameSession.getVendorPlatformCode());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("session", gameSession.getToken());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        String secretKey = credentials.get(Credentials.API_TOKEN);
        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info(formData.toString());

        // TODO: need to add error handling
        GameUrlVendorResponseVo responseVo = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .header("Authorization", secretKey)
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
