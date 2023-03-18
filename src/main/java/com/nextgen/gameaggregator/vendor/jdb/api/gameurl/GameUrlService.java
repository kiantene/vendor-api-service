package com.nextgen.gameaggregator.vendor.jdb.api.gameurl;

import com.alibaba.fastjson.JSONObject;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    private VendorService vendorService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        return null;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        JSONObject json = new JSONObject();
        json.put("action", 21);
        json.put("ts", System.currentTimeMillis());
        json.put("parent", "zt001cnyuatag");
        json.put("uid", gameSession.getVendorPlayerUsername());
        json.put("balance", 0);
        json.put("gType", "7");
        json.put("mType", gameSession.getVendorGameCode());
        json.put("windowMode", "2");

        GameUrlVo vo = new GameUrlVo();

        try {
            String x = vendorService.encrypt(json.toString(), "47e0cd2ece0883e2", "b87f2867577b68ce");

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("dc", "zfs");
            params.add("x", x);

            vo = WebClient.create("http://api.jygrq.com/apiRequest.do")
                    .post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .onStatus(HttpStatus::isError,
                            response -> {
                                HttpStatus clientResponseStatus = response.statusCode();
                                return response.bodyToMono(String.class).map(body ->
                                        new InvalidVendorResponseException
                                                ("response status :" + clientResponseStatus + ", response body :" + body));
                            })
                    .bodyToMono(GameUrlVo.class)
                    .block();

        } catch (Exception ex) {
            throw new InvalidVendorResponseException(ex.getMessage());
        }

        return vo;
    }
}
