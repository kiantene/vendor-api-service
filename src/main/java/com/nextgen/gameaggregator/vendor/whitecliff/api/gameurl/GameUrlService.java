package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.Credentials;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.whitecliff.service.VendorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@Getter
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {
    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private GameSessionService gameSessionService;

    private String agCode;
    private String agToken;
    private String productId;
    GameUrlVo gameUrlVo;


    public GameUrlService() {
        super(GameUrlVo.class);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.LAUNCH_GAME);
    }


    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        this.agCode = ValidationUtils.validateCredential(credentials.get(Credentials.AG_CODE));
        this.agToken = ValidationUtils.validateCredential(credentials.get(Credentials.AG_TOKEN));
        this.productId = ValidationUtils.validateCredential(credentials.get(Credentials.PRODUCT_ID));

        // set DTO
        UserDto userDto = VendorService.setUserDto(gameSession);
        PrdDto prdDto = VendorService.setPrdDto(gameSession, this.getProductId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", userDto);
        body.put("prd", prdDto);

        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("body", new Gson().toJson(body));
        return formData;
    }

    @Override
    protected HttpHeaders getHeaders(HttpHeaders httpHeaders, MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add("ag-code", this.getAgCode());
        headerMap.add("ag-token", this.getAgToken());

        return new HttpHeaders(headerMap);
    }

    @Override
    protected BodyInserter<?, ? super ClientHttpRequest> getBody(MultiValueMap<String, String> formData) {

        return BodyInserters.fromValue(formData.get("body").get(0));
    }

    @Override
    protected GameUrlVo onResponseSuccess(GameUrlVo responseVo,GameSession gameSession) {
        gameSessionService.regenerateVendorToken(gameSession, String.valueOf(responseVo.getUserId()));
        return responseVo;
    }
}