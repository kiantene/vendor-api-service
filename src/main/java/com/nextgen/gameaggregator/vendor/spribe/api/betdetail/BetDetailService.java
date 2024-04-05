package com.nextgen.gameaggregator.vendor.spribe.api.betdetail;

import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.spribe.constant.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials,
                                                         IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        // Get operator
        String operator = Optional.ofNullable(credentials.get(Credentials.OPERATOR)).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("round_id", iBetDetailUrlInfo.getExternalRoundId());
        formData.add("game", iBetDetailUrlInfo.getGameCode().split("_")[1]);
        formData.add("session_token", iBetDetailUrlInfo.getGameSessionToken()); // player_token or session_token can be used
        formData.add("op_player_id", iBetDetailUrlInfo.getVendorUsername());
        formData.add("operator", operator);

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        String apiUrl = Optional.ofNullable(credentials.get(Credentials.API_URL)).orElseThrow(InvalidVendorLineException::new);

        BetDetailUrlVo responseVo = new BetDetailUrlVo();

        // Build uri with formData
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = ResponseEntity.ok().body(uri.toString());

        long endTime = System.currentTimeMillis();
        
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                uri.getPath(), uri.getScheme() + "://" + uri.getHost(), formData, apiResponse, new LinkedMultiValueMap<>(), startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo.setUrl(uri.toString());

            //2. validate vendor response
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, new GameSession());
            throw new InvalidVendorResponseException(apiResponse.toString());
        }

        return responseVo;
    }
}
