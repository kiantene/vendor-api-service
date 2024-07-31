package com.nextgen.gameaggregator.vendor.facai.api.gameurl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.GameType;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    private static final String AGENT_CODE = "AgentCode";
    private static final String CURRENCY = "Currency";
    private static final String PARAMS = "Params";
    private static final String SIGN = "Sign";

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_JSON);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        VendorService vendorService = new VendorService();

        //map request param and convert to json string
        Map<String, Object> loginParam = new HashMap<>();
        loginParam.put("MemberAccount", gameSession.getVendorPlayerUsername());
        loginParam.put("GameID", gameSession.getVendorGameCode());
        loginParam.put("LanguageID", gameSession.getVendorLanguageCode());
        loginParam.put("JackpotStatus", GameType.ENABLE_JACKPOT);
        String jsonParamString = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonParamString = objectMapper.writeValueAsString(loginParam);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("Json Convert Failed");
        }

        //encrypt request param
        String encryptParam = "";
        try {
            encryptParam = vendorService.aesEncrypt(jsonParamString, credentials.get(Credentials.AGENT_KEY));
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("Param Encrypt Failed");
        }

        //MD5 encrypt
        String md5Param = "";
        try {
            md5Param = DigestUtils.md5Hex(jsonParamString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("MD5 Encrypt Failed");
        }

        //setup form data
        formData.add(AGENT_CODE, credentials.get(Credentials.AGENT_CODE));
        formData.add(CURRENCY, gameSession.getVendorCurrencyCode());
        formData.add(PARAMS, encryptParam);
        formData.add(SIGN, md5Param);

        return formData;
    }

//    @Override
//    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
//            throws InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {
//
//        String apiUrl = Optional.ofNullable(credentials.get(Credentials.API_URL))
//                .orElseThrow(InvalidVendorLineException::new);
//
//        GameUrlVo responseVo;
//        AtomicBoolean isTimeout = new AtomicBoolean(false);
//
//        //post request to vendor API with JSON string
//        ResponseEntity<String> response = WebClient.create(apiUrl)
//                .post()
//                .uri(EndPoints.GAME_URL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromValue(formData.toSingleValueMap()))
//                .retrieve()
//                .toEntity(String.class)
//                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
//                .onErrorResume(TimeoutException.class, e -> {
//                    isTimeout.set(true);
//                    return Mono.error(e);
//                })
//                .block();
//
//        String responseBody = GameUrl.validateResponse(response, isTimeout, httpRequestLog);
//        try {
//            responseVo = new ObjectMapper().readValue(responseBody, GameUrlVo.class);
//
//        } catch (JsonProcessingException exception) {
//            throw new InvalidVendorResponseException(exception.getMessage());
//        }
//
//        return responseVo;
//    }
}
