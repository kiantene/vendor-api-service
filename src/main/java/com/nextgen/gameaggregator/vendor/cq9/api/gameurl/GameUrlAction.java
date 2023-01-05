package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
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
public class GameUrlAction implements GameUrl {
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("account", gameSession.getVendorPlayerUsername());
        formData.add("gamehall", "CQ9");
        formData.add("gamecode", gameCode);
        formData.add("gameplat", "WEB");
        formData.add("lang", gameSession.getLanguage());
        formData.add("session", gameSession.getToken());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + Endpoints.GAME_URL);
        log.info(formData.toString());

        // TODO: need to add error handling
        ResponseVo responseVo = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.GAME_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(ResponseVo.class)
                .block();

        return (GameUrlVo) responseVo.getData();
    }
}
