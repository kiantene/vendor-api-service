package com.nextgen.gameaggregator.vendor.jdb.api.betdetail;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.jdb.constant.Actions;
import com.nextgen.gameaggregator.vendor.jdb.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;

import reactor.core.publisher.Mono;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials,
            IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {
        
        String[] parts = iBetDetailUrlInfo.getGameCode().split("_");
        int gType = Integer.parseInt(parts[1]);
        
        BetDetailDto dto = new BetDetailDto();
        dto.setAction(Actions.BET_DETAIL_URL);
        dto.setTs(System.currentTimeMillis());
        dto.setParent(credentials.get(Credentials.PARENT));
        dto.setUid(iBetDetailUrlInfo.getVendorUsername());
        dto.setLang(vendorLanguageCode.getLanguageCode());
        dto.setGType(gType);
        dto.setSeqNo(iBetDetailUrlInfo.getExternalRoundId());
        dto.setShowUid(1);

        Gson gson = new GsonBuilder().create();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        try {
            String x = VendorService.encrypt(gson.toJson(dto), credentials.get(Credentials.KEY), credentials.get(Credentials.IV));

            formData.add("dc", credentials.get(Credentials.DC));
            formData.add("x", x);

        }  catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
            IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

                String apiUrl = credentials.get(Credentials.API_SERVER);
                Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        
                BetDetailUrlVo responseVo = null;
                MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

                URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();
        
                long startTime = System.currentTimeMillis();
                ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                        .get()
                        .uri(uri)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                        .toEntity(String.class)
                        .retry(3)
                        .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                        .block();
        
                long endTime = System.currentTimeMillis();
                RequestLogVo requestLogVo = requestService.createRequestLogVo(
                        "", apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                        this.getClass().getPackage().getName(), profilesActive);
        
                try {
        
                    // 1. validate HTTP Response Code
                    requestService.validateVendorHttpStatusResponse(apiResponse);
                    responseVo = new Gson().fromJson((String) apiResponse.getBody(), BetDetailUrlVo.class);
        
                    //2. validate vendor response
                    Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
                    RequestService.validateResponse(responseVo);
                    RequestService.successResponseLog(requestLogVo);
        
                } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
                    RequestService.failResponseLog(requestLogVo, invalidException);
                    throw new InvalidVendorResponseException();
                }
        
                return responseVo;
    }
    
}
