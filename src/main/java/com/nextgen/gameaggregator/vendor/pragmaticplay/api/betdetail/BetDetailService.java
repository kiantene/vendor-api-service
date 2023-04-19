package com.nextgen.gameaggregator.vendor.pragmaticplay.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.repository.VendorPlayerRepository;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    private VendorPlayerRepository vendorPlayerRepository;

    @Override
    public MultiValueMap<String, String> formDataBuilder( Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {
        String secureLogin = credentials.get(Credentials.SECURE_LOGIN);
        Optional.ofNullable(secureLogin).orElseThrow(InvalidVendorLineException::new);

        String secret = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secret).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("secureLogin", secureLogin);
        formData.add("playerId", iBetDetailUrlInfo.getVendorUsername());
        formData.add("roundId", iBetDetailUrlInfo.getExternalRoundId());
        String hash = VendorService.generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        String apiUrl = credentials.get(Credentials.REPORT_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);


        ResponseEntity apiResponse =  WebClient.create("https://stg.gasea168.com/")
                .post()
                .uri(Endpoints.OPEN_HISTORY+"sdfsd")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();


        Gson gson = new Gson();
        System.err.println();
        System.err.println(apiUrl);
        System.err.println(gson.toJson(formData));
        System.err.println(apiResponse.getBody());

        BetDetailUrlVo responseVo = null;
        try {
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), BetDetailUrlVo.class);

        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new InvalidVendorResponseException( "Invalid vendor response body :"+apiResponse.getBody());
        }
        System.err.println(responseVo);
        return responseVo;
    }
}
