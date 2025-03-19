package com.nextgen.gameaggregator.vendor.amusnet.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.amusnet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.amusnet.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    @Autowired
    private VendorService vendorService;
    private String gameUrl;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setCredentialApiUrl(Credentials.GAME_URL);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {

        // If have data then open with portalCodeEQGame
        // using vendorGameCode to verify(540,539,542,543,541,538)
        String categoryCodeList = ValidationUtils.validateCredential(credentials.get(Credentials.CATEGORY_CODE_EQ));
        String portalCodeEQ = ValidationUtils.validateCredential(credentials.get(Credentials.PORTAL_CODE_EQ));
        String portalCode = ValidationUtils.validateCredential(credentials.get(Credentials.PORTAL_CODE));
        String verifiedPortalCode = ValidationUtils.validateCredential(vendorService.checkGameCodeIsOpenEQGame
                (categoryCodeList, gameSession.getVendorGameCode(), portalCodeEQ, portalCode));
        this.gameUrl = ValidationUtils.validateCredential(credentials.get(Credentials.GAME_URL));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("defenceCode", gameSession.getVendorToken());
        formData.add("playerId", gameSession.getVendorPlayerUsername());
        formData.add("portalCode", verifiedPortalCode);
        formData.add("screenName", gameSession.getVendorPlayerUsername());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("country", "MY");
        formData.add("gameId", gameSession.getVendorGameCode());

        return formData;
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        httpRequestLog.setUrl(this.gameUrl);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        URI uri = UriComponentsBuilder.fromUriString(this.gameUrl)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> response = ResponseEntity.ok().body(uri.toString());

        this.validateResponse(response, isTimeout, httpRequestLog, GameUrlVo.class, gameSession);

        GameUrlVo responseVo = new GameUrlVo();
        responseVo.setUrl(uri.toString());

        return responseVo;
    }

}
