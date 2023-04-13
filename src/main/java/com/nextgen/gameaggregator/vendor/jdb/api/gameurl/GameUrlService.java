package com.nextgen.gameaggregator.vendor.jdb.api.gameurl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.fastjson.JSONObject;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.jdb.constant.Actions;
import com.nextgen.gameaggregator.vendor.jdb.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    private VendorService vendorService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = gameCode.split("_");
        int gType = Integer.parseInt(parts[0]);
        int mType = Integer.parseInt(parts[1]);
        
        JSONObject json = new JSONObject();
        json.put("action", Actions.GAME_URL);
        json.put("ts", System.currentTimeMillis());
        json.put("parent", credentials.get(Credentials.PARENT));
        json.put("uid", gameSession.getVendorPlayerUsername());
        json.put("balance", 0);
        json.put("lang", gameSession.getVendorLanguageCode());
        json.put("gType", gType);
        json.put("mType", mType);
        json.put("windowMode", "2");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        try {
            String x = VendorService.encrypt(json.toString(), credentials.get(Credentials.KEY), credentials.get(Credentials.IV));

            params.add("dc", credentials.get(Credentials.DC));
            params.add("x", x);

        }  catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }

        return params;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        GameUrlVo vo = new GameUrlVo();

        try {
            vo = WebClient.create(credentials.get(Credentials.API_SERVER))
                    .post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    // TODO: to catch more error codes
                    .onStatus(HttpStatus.BAD_REQUEST::equals, response -> Mono.empty())
//                    .onStatus(HttpStatus::isError,
//                            response -> {
//                                HttpStatus clientResponseStatus = response.statusCode();
//                                return response.bodyToMono(String.class).map(body ->
//                                        new InvalidVendorResponseException
//                                                ("response status :" + clientResponseStatus + ", response body :" + body));
//                            })
                    .bodyToMono(GameUrlVo.class)
                    .block();

        } catch (Exception ex) {
            throw new InvalidVendorResponseException(ex.getMessage());
        }

        return vo;
    }
}
