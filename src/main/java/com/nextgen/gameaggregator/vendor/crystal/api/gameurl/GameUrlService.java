package com.nextgen.gameaggregator.vendor.crystal.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.crystal.constant.Credentials;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.crystal.service.VendorService;
import org.jvnet.hk2.annotations.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
public class GameUrlService extends BaseGameUrlService<CrystalGameUrlVo> {

    public GameUrlService() {
        super(CrystalGameUrlVo.class);
        this.setCredentialApiUrl(Credentials.GAME_URL);
        this.setGameUrl(EndPoints.LAUNCH_GAME);
    }

    @Override
    protected HttpHeaders getHeaders(HttpHeaders httpHeaders, MultiValueMap<String, String> formData,
                                     Map<String, String> credentials, GameSession gameSession) {

        HttpHeaders headers = new HttpHeaders();
        String secretKey = credentials.getOrDefault(Credentials.SECRET_KEY, "");
        String operatorCode = credentials.getOrDefault(Credentials.OPERATOR_CODE, "");
        String compactJson = VendorService.convertToCompactJson(formData);
        String signature = VendorService.hashHMACSha256(compactJson, secretKey);

        headers.add("X-SIGNATURE", signature);
        headers.add("OPERATOR", operatorCode);
        return headers;
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        String brandCode = credentials.getOrDefault(Credentials.BRAND_CODE, "");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("gameCode", gameSession.getVendorGameCode());
        formData.add("brandCode", brandCode);
        formData.add("currencyCode", gameSession.getVendorCurrencyCode());
        formData.add("playerId", String.valueOf(gameSession.getVendorPlayerUsername()));
        return formData;
    }
}