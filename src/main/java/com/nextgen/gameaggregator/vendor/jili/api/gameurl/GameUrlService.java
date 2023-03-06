package com.nextgen.gameaggregator.vendor.jili.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.jili.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jili.service.VendorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @SneakyThrows
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {
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

    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        URI uri = UriComponentsBuilder.fromUriString(apiUrl + EndPoints.GAME_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

//        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
//        log.info(formData.toString());

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(uri.toString())
                .build();

        String responseString = "";
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseString = response.body().string();
            } else {
                throw new InvalidVendorResponseException("Request failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


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
