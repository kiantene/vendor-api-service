package com.nextgen.gameaggregator.vendor.facai.api.gameurl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        try {
            VendorService vendorService = new VendorService();

            //map request param and convert to json string
            Map<String, String> loginParam = new HashMap<String, String>();
            loginParam.put("MemberAccount",gameSession.getVendorPlayerUsername());
            loginParam.put("GameID",gameCode);
            loginParam.put("LanguageID",gameSession.getVendorLanguageCode());
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonParam = objectMapper.writeValueAsString(loginParam);

            //encrypt request param
            String md5Param = vendorService.md5(jsonParam);
            String encryptParam = vendorService.aesEncrypt(jsonParam, credentials.get(Credentials.AGENT_KEY));

            //setup form data
            formData.add("AgentCode", credentials.get(Credentials.AGENT_CODE));
            formData.add("Currency", gameSession.getVendorCurrencyCode());
            formData.add("Params", encryptParam);
            formData.add("Sign", md5Param);
        } catch (Exception exception) { // any other exception encountered
            log.info("error encrypt");
        }

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorLineException, InvalidVendorResponseException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info(formData.toString());

        try {
            //convert from data into mapper data
            Map<String, String> convertFormMap = new HashMap<String, String>();
            convertFormMap.put("AgentCode",formData.getFirst("AgentCode"));
            convertFormMap.put("Currency",formData.getFirst("Currency"));
            convertFormMap.put("Params",formData.getFirst("Params"));
            convertFormMap.put("Sign",formData.getFirst("Sign"));
            //convert mapper data into json string
            ObjectMapper objectMapper = new ObjectMapper();
            String fromJson = objectMapper.writeValueAsString(convertFormMap);
            log.info("rawJson : " + fromJson);

            GameUrlVo responseVo = WebClient.create(apiUrl)
                    .post()
                    .uri(EndPoints.GAME_URL)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(BodyInserters.fromObject(fromJson))
                    //.contentType(MediaType.APPLICATION_JSON)
                    //.body(BodyInserters.fromValue(formData))
                    .retrieve()
                    .bodyToMono(GameUrlVo.class)
                    .block();

            log.info("responseVo : " + responseVo.toString());
            if (responseVo.getUrl() != null) {
                log.info(responseVo.toString());
            } else {
                throw new InvalidVendorResponseException("Invalid Response : " + responseVo.toString());
            }

            return responseVo;

        } catch (Exception exception) { // any other exception encountered
            log.info("error convert");

            GameUrlVo responseVo = new GameUrlVo();
            
            throw new InvalidVendorResponseException("Invalid Response : " + responseVo.toString());
        }

        //return responseVo;

    }
}
