package com.nextgen.gameaggregator.vendor.jili.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.jili.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jili.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        String agentId = credentials.get(Credentials.AGENT_ID);
        Optional.ofNullable(agentId).orElseThrow(InvalidVendorLineException::new);

        String agentKey = credentials.get(Credentials.AGENT_KEY);
        Optional.ofNullable(agentKey).orElseThrow(InvalidVendorLineException::new);

        VendorService service = new VendorService();
        service.setAgentId(agentId);
        service.setAgentKey(agentKey);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Token", gameSession.getToken());
        formData.add("GameId", gameSession.getVendorGameCode());
        formData.add("Lang", gameSession.getVendorLanguageCode());
        formData.add("AgentId", agentId);
        String key = service.keyGenerator(formData);
        formData.add("Key", key);

        return formData;
    }

    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.GAME_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info(formData.toString());

        // TODO: need to add error handling
        String responseString = WebClient.create()
                .get()
                .uri(uri)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        GameUrlVo responseVo = null;
        try {
            responseVo = new Gson().fromJson(responseString, GameUrlVo.class);
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new InvalidVendorResponseException( "Invalid vendor response body :"+responseString);
        }

        if (responseVo.getErrorCode() != ResponseCode.SUCCESS.errorCode) {
            throw new InvalidVendorResponseException( "Invalid vendor response" );
        }

        return responseVo;
    }
}
