package com.nextgen.gameaggregator.vendor.aviatorstudio.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Service
public class GameUrlService extends BaseGameUrlService<ASGameUrlVo> {

    public GameUrlService() {
        super(ASGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        try {
            String providerId = ValidationUtils.validateCredential(credentials.get(Credentials.PROVIDER_ID));
            String publicKey = ValidationUtils.validateCredential(credentials.get(Credentials.PUBLIC_KEY));
            String jwtToken = ValidationUtils.validateCredential(credentials.get(Credentials.JWT_SECRET));
            String userid = gameSession.getVendorPlayerUsername();
            String sessionId = gameSession.getToken();
            String token = VendorService.generateJWT(userid, sessionId, jwtToken, publicKey);

            formData.add("token", token);
            formData.add("providerId", providerId);
            formData.add("currency", gameSession.getVendorCurrencyCode());
            formData.add("language", gameSession.getVendorLanguageCode());
            formData.add("gameId", gameCode);
        } catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }

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
