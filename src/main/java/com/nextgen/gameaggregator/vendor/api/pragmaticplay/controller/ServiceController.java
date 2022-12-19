package com.nextgen.gameaggregator.vendor.api.pragmaticplay.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "api/v1/")
public class ServiceController {

    RestTemplate restTemplate;

    @PostMapping(path = "game/getGameList")
    public JsonNode getGameList() throws JsonProcessingException {
        String url = "https://api.prerelease-env.biz/IntegrationService/v3/http/CasinoGameAPI/getCasinoGames/";
        String secretKey = "testKey";
        RestTemplate restTemplate = new RestTemplate();

        // create a map to store the request parameters
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secureLogin", "zf06_rtw015sw");
        params.add("hash", this.generateHash(params, secretKey));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // create the request object
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        // make the POST request and store the response
        String response = restTemplate.postForObject(url, requestEntity, String.class);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonResponse = mapper.readTree(response);

        return jsonResponse;
    }

    private String generateHash(MultiValueMap<String, String> params, String secret) {
        String payload = params.keySet().stream()
                .sorted()
                .map(key -> key + "=" + params.get(key).get(0))
                .collect(Collectors.joining("&"));

        payload += secret;
        return DigestUtils.md5Hex(payload);
    }

}
