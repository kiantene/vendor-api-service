package com.nextgen.gameaggregator.vendor.api.pragmaticplay.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "api/v1/")
public class ServiceController extends HttpServlet {

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

    @PostMapping(path = "game/getGameIcon")
    public JsonNode getGameIcon(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String imagePath = "https://zf006.prerelease-env.biz";
        GetGameListVo vo = new GetGameListVo();

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        String payload = sb.toString();

        Gson gson = new Gson();
        GetGameListDto obj = gson.fromJson(payload, GetGameListDto.class);

        vo = this.generateGameListPath(imagePath, obj.vendorGameCode);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse = mapper.valueToTree(vo);

        return jsonResponse;
    }

    @PostMapping(path = "bet/detail")
    public JsonNode getBetDetails(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String imagePath = "https://zf006.prerelease-env.biz";
        GetGameListVo vo = new GetGameListVo();

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        String payload = sb.toString();

        Gson gson = new Gson();
        GetBetDetailDto obj = gson.fromJson(payload, GetBetDetailDto.class);

        String url = "https://api.prerelease-env.biz/IntegrationService/v3/http/HistoryAPI/OpenHistoryExtended/";
        String secretKey = "testKey";
        RestTemplate restTemplate = new RestTemplate();
        String secureLogin = "zf06_rtw015sw";

        if(obj.agentId.toString() == "2"){
            secureLogin = "zf_winksw";
        }

        // create a map to store the request parameters
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secureLogin", secureLogin);
        params.add("playerId", obj.vendorUsername);
        params.add("gameId", obj.vendorGameCode);
        params.add("roundId", obj.roundId);
        params.add("hash", this.generateHash(params, secretKey));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // create the request object
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        // make the POST request and store the response
        String result = restTemplate.postForObject(url, requestEntity, String.class);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonResponse = mapper.readTree(result);

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

    private GetGameListVo generateGameListPath(String imagePath, String vendorGameCode){

        String rec325 = imagePath+"/game_pic/rec/325/"+vendorGameCode+".png";
        String rec188 = imagePath+"/game_pic/rec/188/"+vendorGameCode+".png";
        String rec160 = imagePath+"/game_pic/rec/160/"+vendorGameCode+".png";
        String square200 = imagePath+"/game_pic/square/200/"+vendorGameCode+".png";
        String square138 = imagePath+"/game_pic/square/138/"+vendorGameCode+".jpg";

        GetGameListVo getGameListVo = new GetGameListVo();
        getGameListVo.setImage325x234(rec325);
        getGameListVo.setImage188x83(rec188);
        getGameListVo.setImage160x115(rec160);
        getGameListVo.setImage200x200(square200);
        getGameListVo.setImage138x138(square138);

        return getGameListVo;
    }

    private static class GetGameListDto {
        String vendorCode;
        String vendorGameCode;
    }

    private static class GetBetDetailDto {
        String vendorCode;
        String vendorGameCode;
        String roundId;
        String vendorUsername;
        String agentId;
    }

    private static class GetGameListVo {
        String image325x234;
        String image188x83;
        String image160x115;
        String image200x200;
        String image138x138;

        public String getImage325x234() {
            return image325x234;
        }

        public void setImage325x234(String image325x234) {
            this.image325x234 = image325x234;
        }

        public String getImage188x83() {
            return image188x83;
        }

        public void setImage188x83(String image188x83) {
            this.image188x83 = image188x83;
        }

        public String getImage160x115() {
            return image160x115;
        }

        public void setImage160x115(String image160x115) {
            this.image160x115 = image160x115;
        }

        public String getImage200x200() {
            return image200x200;
        }

        public void setImage200x200(String image200x200) {
            this.image200x200 = image200x200;
        }

        public String getImage138x138() {
            return image138x138;
        }

        public void setImage138x138(String image138x138) {
            this.image138x138 = image138x138;
        }
    }

}
