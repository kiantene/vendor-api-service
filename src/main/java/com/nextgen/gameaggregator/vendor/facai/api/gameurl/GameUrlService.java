package com.nextgen.gameaggregator.vendor.facai.api.gameurl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.S3Service;
import com.nextgen.gameaggregator.util.EncryptionUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.Encryption;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.GameType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    @Autowired
    private S3Service s3Service;

    private static final String AGENT_CODE = "AgentCode";
    private static final String CURRENCY = "Currency";
    private static final String PARAMS = "Params";
    private static final String SIGN = "Sign";

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_JSON);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        //map request param and convert to json string
        Map<String, Object> loginParam = new HashMap<>();
        loginParam.put("MemberAccount", gameSession.getVendorPlayerUsername());
        loginParam.put("GameID", gameSession.getVendorGameCode());
        loginParam.put("LanguageID", gameSession.getVendorLanguageCode());
        loginParam.put("JackpotStatus", GameType.ENABLE_JACKPOT);
        loginParam.put("ClientIP", gameSession.getIpAddress());
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
            String secret = credentials.get(Credentials.AGENT_KEY);
            encryptParam = EncryptionUtils.aesEncrypt(Encryption.CIPHER_MODE_AND_PADDING, jsonParamString, secret);
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

    @Override
    public GameUrlVo responseMapper(String responseBody, GameSession gameSession) {
        GameUrlVo gameUrlVo = new GameUrlVo();
        String vendorGameUrl = s3Service.generateHtmlToS3(gameSession, responseBody);
        gameUrlVo.setUrl(vendorGameUrl);

        return gameUrlVo;
    }
}
