package com.nextgen.gameaggregator.vendor.aviatorstudio.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class GameUrlService extends BaseGameUrlService<ASGameUrlVo> {
    private final VendorService vendorService;

    public GameUrlService(VendorService vendorService) {
        super(ASGameUrlVo.class);
        this.setAutoMapResponse(false);
        this.vendorService = vendorService;
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws Exception {
        String providerId = ValidationUtils.validateCredential(credentials.get(Credentials.PROVIDER_ID));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", vendorService.generateJWT(gameSession.getVendorPlayerUsername(), gameSession.getToken(), gameSession.getVendorLineId()));
        formData.add("providerId", providerId);
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("gameId", gameCode);

        return formData;
    }

    @Override
    public ASGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                    GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        //construct API address
        String launchUrl = Optional.of(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);

        URI url = UriComponentsBuilder.fromUriString(launchUrl)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        ASGameUrlVo responseVo = new ASGameUrlVo();

        responseVo.setGameUrl(url.toString());
        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }

}
