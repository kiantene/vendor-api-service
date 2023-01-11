package com.nextgen.gameaggregator.vendor.pragmaticplay.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
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
        // TODO: send platform value, if not supported then send web as default
        // Possible values for platform – WEB (for desktop devices) or MOBILE (for mobile devices)
        String hash = VendorService.generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }

    @Override
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
}
