package com.nextgen.gameaggregator.vendor.pragmaticplay.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.VendorPlayer;
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
        VendorPlayer vendorPlayer =vendorPlayerRepository.findById(iBetDetailUrlInfo.getVendorPlayerId()).orElseThrow(RecordNotFoundException::new);

        formData.add("secureLogin", secureLogin);
        formData.add("playerId", vendorPlayer.getUsername());
        formData.add("roundId", iBetDetailUrlInfo.getExternalRoundId());
        String hash = VendorService.generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
System.err.println("AAAAAAA");
        ResponseEntity apiResponse =  WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.GAME_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        System.err.println(apiResponse.getBody());
        BetDetailUrlVo responseVo = null;
        try {
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), BetDetailUrlVo.class);

        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new InvalidVendorResponseException( "Invalid vendor response body :"+apiResponse.getBody());
        }
        return responseVo;
    }
}
