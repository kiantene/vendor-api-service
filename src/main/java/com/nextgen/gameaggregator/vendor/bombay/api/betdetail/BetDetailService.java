package com.nextgen.gameaggregator.vendor.bombay.api.betdetail;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.vendor.bombay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bombay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bombay.service.VendorService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.Security;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    VendorService vendorService;

    @Autowired
    RequestService requestService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        //setup form data
        formData.add("operator_id", credentials.get(Credentials.operator_id));
        formData.add("round_id", iBetDetailUrlInfo.getExternalRoundId());
        formData.add("user_id", iBetDetailUrlInfo.getVendorUsername());

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        BetDetailUrlVo responseVo = new BetDetailUrlVo();

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        // convert multi value map into hash map
        Map<String, Object> hashMap = vendorService.convertToHashMap(formData);

        //construct API address
        String urlScheme = credentials.get(Credentials.api_url);

        //check vendor status in our DB
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        // convert operator id from string into int
        hashMap.put("operator_id", Integer.valueOf((String) hashMap.get("operator_id")));

        // Convert Map to JSON string using Gson
        Gson gson = new Gson();
        String forDataToString = gson.toJson(hashMap);

        String private_key = credentials.get(Credentials.private_key);

        String signature = null;

        // let SHA256 or RSA ignore and bypass the cert(vendor only provide private and public key)
        Security.addProvider(new BouncyCastleProvider());

        try{

            signature = vendorService.generateSignature(forDataToString, private_key);

        }catch(Exception e){
            throw new RuntimeException("Error generating signature", e);
        }

        // Assign value for header
        headerMap.add("X-Signature", signature);

        //Construct the API to get game url from vendor site(those parameter get from formDataBuilder function)
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.BET_DETAIL)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(forDataToString)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        responseVo = new Gson().fromJson((String) apiResponse.getBody(), BetDetailUrlVo.class);

        return responseVo;
    }
}
