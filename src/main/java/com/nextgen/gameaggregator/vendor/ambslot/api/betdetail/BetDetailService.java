package com.nextgen.gameaggregator.vendor.ambslot.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.ambslot.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ambslot.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ambslot.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    VendorService vendorService;
    @Autowired
    RequestService requestService;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        //setup form data
        formData.add("tranId", iBetDetailUrlInfo.getTransactionId());
        formData.add("agent",  credentials.get(Credentials.prefix));
        formData.add("username", iBetDetailUrlInfo.getVendorUsername());
        formData.add("language", vendorLanguageCode.getLanguageCode());
        formData.add("provider", "AMBSLOT");
        formData.add("type", "slot");

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        BetDetailUrlVo responseVo = new BetDetailUrlVo();

        //BetDetails do not have game session;
        GameSession gameSession = new GameSession();

        //construct API address
        String urlScheme = credentials.get(Credentials.api_url);

        //check vendor status in our DB
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        //Construct the API to get game url from vendor site(those parameter get from formDataBuilder function)
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.BET_DETAILS)
                .build()
                .encode()
                .toUri();

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        String formdataJSON = vendorService.convertMapToJson(formData);

        String secret = credentials.get(Credentials.secret);
        int iterations = 1000;

        // Generate x-ambslot-signature value for create member
        String encrypted_value = vendorService.encryption(formdataJSON, secret, iterations);

        // Assign value for header
        headerMap.add("x-ambslot-signature", encrypted_value);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(formdataJSON)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();

        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.LAUNCH_GAME, urlScheme, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try{
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), BetDetailUrlVo.class);

            System.out.println(responseVo);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);
        }catch(HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException){
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }
}
