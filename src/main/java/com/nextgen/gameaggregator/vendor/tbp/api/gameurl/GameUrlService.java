package com.nextgen.gameaggregator.vendor.tbp.api.gameurl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.Credentials;
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@Getter
public class GameUrlService extends BaseGameUrlService<TBPGameUrlVo> {

    public GameUrlService() {
        super(TBPGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        String token = ValidationUtils.validateCredential(credentials.get(Credentials.TOKEN));

        formData.add("PlayerToken", gameSession.getVendorPlayerUsername());
        formData.add("PlayerName", gameSession.getVendorPlayerUsername());
        formData.add("OperatorToken", token);
        formData.add("GameToken", gameSession.getVendorGameCode());
        formData.add("Currency", gameSession.getVendorCurrencyCode());
        formData.add("Language", gameSession.getVendorLanguageCode());
        formData.add("DefenceCode", gameSession.getToken());

        return formData;
    }

    @Override
    public TBPGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                     GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, InvalidVendorLineException, TimeoutException {
        //construct API address
        String baseUrl = Optional.ofNullable(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);

        // Trigger create Game List function by calling vendor api
        String source = getSourceProperty(credentials, gameSession.getVendorGameCode());

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(EndPoints.AUTHORIZE)
                .build()
                .encode()
                .toUri();

        // Trigger doPost to get session id by calling vendor api
        ResponseEntity<String> apiResponse = this.doPost(baseUrl, uri.toString(), new HttpHeaders(), formData, isTimeout);

        this.validateResponse(apiResponse, isTimeout, httpRequestLog, TBPGameUrlVo.class, gameSession);

        TBPGameUrlVo responseVo;
        try {
            responseVo = new ObjectMapper().readValue(apiResponse.getBody(), TBPGameUrlVo.class);
        } catch (Exception e) {
            throw new InvalidVendorResponseException("Failed to parse vendor response");
        }

        String gameUrl = UriComponentsBuilder.fromUriString(source)
                .queryParam("sessionId", responseVo.getData().getSessionId())
                .queryParam("homeUrl", gameSession.getLobbyUrl())
                .queryParam("language", gameSession.getVendorLanguageCode())
                .build()
                .toUriString();

        responseVo.setGameUrl(gameUrl);
        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }

    private String getSourceProperty(Map<String, String> credentials, String gamecode)
            throws InvalidVendorLineException, InvalidVendorResponseException {
        //construct API address & check vendor status in our DB
        String urlScheme = Optional.of(credentials.get(Credentials.LAUNCH_URL))
                .orElseThrow(InvalidVendorLineException::new);
        String token = Optional.ofNullable(credentials.get(Credentials.TOKEN))
                .orElseThrow(InvalidVendorLineException::new);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("Token", token);

        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.GAME_LIST)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(this.getBody(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        if (apiResponse == null || apiResponse.getBody() == null) {
            throw new InvalidVendorResponseException("Unable to get Game List");
        }
        DataDto dataDto;
        try {
            dataDto = new ObjectMapper().readValue(apiResponse.getBody(), DataDto.class);
        } catch (Exception e) {
            throw new InvalidVendorResponseException("Failed to parse Game List response");
        }
        String source = "";
        for (GameDto game : dataDto.getData()) {
            if (game.getToken().equals(gamecode)) source = game.getSource();
        }

        return source;
    }
}
