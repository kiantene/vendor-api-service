package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.Map;


@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setHttpMethod(HttpMethod.GET);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        String key = ValidationUtils.validateCredential(credentials.get(Credentials.KEY));
        String projId = ValidationUtils.validateCredential(credentials.get(Credentials.PROJ_ID));

        GameUrlDto gameUrlDto = VendorService.getGameUrlDto(gameSession, projId);

        Map<String, Object> mapData = VendorService.convertObjectToMap(gameUrlDto, LinkedHashMap.class);
        VendorService.rearrangeMap(mapData);
        MultiValueMap<String, String> formData = VendorService.flattenMapIntoMultiValueMap(mapData, "");
        formData.add("signature", VendorService.md5(VendorService.buildSignature(formData, key)));

        return formData;
    }
}
