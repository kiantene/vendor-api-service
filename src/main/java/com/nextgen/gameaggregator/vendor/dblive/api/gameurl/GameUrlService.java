package com.nextgen.gameaggregator.vendor.dblive.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.dblive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dblive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dblive.constant.GameCodes;
import com.nextgen.gameaggregator.vendor.dblive.service.VendorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameUrlService extends BaseGameUrlService<DbLiveGameUrlVo> {

    public GameUrlService() {
        super(DbLiveGameUrlVo.class);
        this.setAutoMapResponse(false);
        this.setCredentialApiUrl(Credentials.APIURL);
        this.setContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        //1.Generate Open GameUrl Data
        GameUrlDto gameUrlData = new GameUrlDto();
        gameUrlData.setLoginName(credentials.get(Credentials.AGENTID) + "_" + gameSession.getVendorPlayerUsername());
        gameUrlData.setLoginPassword(gameSession.getVendorPlayerUsername());
        gameUrlData.setDeviceType(Integer.parseInt(gameSession.getVendorPlatformCode()));
        gameUrlData.setLang(1);
        gameUrlData.setBackurl(gameSession.getLobbyUrl());
        if (!GameCodes.LOBBY.getCode().equalsIgnoreCase(gameSession.getVendorGameCode())) {
            gameUrlData.setGameTypeId(gameSession.getVendorGameCode());
        }
        gameUrlData.setTimestamp(System.currentTimeMillis());
        //GA-12908 adding language selection feature.
        gameUrlData.setPlayerLanguageV2(gameSession.getVendorLanguageCode());

        //2.Encrypt Open GameUrl Data and return
        return encryptApiRequest(gameUrlData, credentials);
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                  GameSession gameSession, HttpRequestLog httpRequestLog) throws
            InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {

        String apiUrl = credentials.get(Credentials.APIURL);

        //1. Trigger create member function before calling gameUrl
        this.createMember(apiUrl, credentials, gameSession);

        //2/ Trigger doPost to get game url function by calling vendor api
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        HttpHeaders httpHeaders = this.getHeaders(new HttpHeaders(), formData, credentials, gameSession);

        ResponseEntity<String> response = this.doPost(apiUrl, EndPoints.LAUNCH_GAME, httpHeaders, formData, isTimeout);

        this.validateResponse(response, isTimeout, httpRequestLog, DbLiveGameUrlVo.class, gameSession);

        DbLiveGameUrlVo responseVo = new Gson().fromJson(response.getBody(), DbLiveGameUrlVo.class);

        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }

    private void createMember(String urlScheme, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorResponseException {

        // Define data for register member
        MemberDto memberDto = new MemberDto();
        memberDto.setLoginName(credentials.get(Credentials.AGENTID) + "_" + gameSession.getVendorPlayerUsername());
        memberDto.setLoginPassword(gameSession.getVendorPlayerUsername());
        memberDto.setLang(1);
        memberDto.setTimestamp(System.currentTimeMillis());

        try {

            //Encrypt register member Data
            MultiValueMap<String, String> formData = encryptApiRequest(memberDto, credentials);

            //Construct the API to register player from vendor site
            AtomicBoolean isTimeout = new AtomicBoolean(false);

            HttpHeaders httpHeaders = this.getHeaders(new HttpHeaders(), formData, credentials, gameSession);

            this.doPost(urlScheme, EndPoints.CREATE_PLAYER, httpHeaders, formData, isTimeout);

        } catch (Exception e) {
            throw new InvalidVendorResponseException();
        }
    }

    private <T> MultiValueMap<String, String> encryptApiRequest(T dto, Map<String, String> credentials) throws InvalidVendorLineException {
        Gson gson = new Gson();
        String source = gson.toJson(dto);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("merchantCode", credentials.get(Credentials.AGENTID));
        formData.add("params", VendorService.encrypt(source, credentials.get(Credentials.AESKEY)));
        formData.add("signature", VendorService.getMD5(source + credentials.get(Credentials.MD5KEY)));
        return formData;
    }
}
