package com.nextgen.gameaggregator.vendor.pgsoft.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

public class BetDetailService implements BetDetailUrl {
    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials,
                                                         IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        String operatorToken = credentials.get(Credentials.OPERATOR_TOKEN);
        Optional.ofNullable(operatorToken).orElseThrow(InvalidVendorLineException::new);

        String secretKey = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);

        String apiUrl = credentials.get(Credentials.PGSOFT_API_DOMAIN);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);


        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("operator_token", operatorToken);
        formData.add("secret_key", secretKey);
        return formData;

    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                               IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        LoginProxyVo loginProxyVo = this.callLoginProxy(formData, credentials);
        BetDetailUrlVo betDetailUrlVo = new BetDetailUrlVo();
        if (loginProxyVo.equals(null)) {
            betDetailUrlVo.setError(loginProxyVo.getError());
        } else {
            String UrlBetHistory = credentials.get(Credentials.PUBLIC_DOMAIN)+Endpoints.BET_DETAIL_STEP_TWO+
                    "?trace_id={0}&t={1}&psid={2}&sid={3}&lang={4}&type=operator";

            String betDetailUrl = VendorService.generateBetDetailUrl(
                    UrlBetHistory, String.valueOf(UUID.randomUUID()), loginProxyVo.getData().getOperator_session(), iBetDetailUrlInfo.getExternalRoundId(),
                    iBetDetailUrlInfo.getExternalTransactionId(), vendorLanguageCode.getLanguageCode());
            betDetailUrlVo.setUrl(betDetailUrl);
        }


        return betDetailUrlVo;
    }

    public LoginProxyVo callLoginProxy(MultiValueMap<String, String> formData, Map<String, String> credentials) throws InvalidVendorResponseException, InvalidVendorLineException {
        String apiUrl = credentials.get(Credentials.PGSOFT_API_DOMAIN) ;
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        String uri = Endpoints.BET_DETAIL_STEP_ONE+ "?trace_id="+String.valueOf(UUID.randomUUID());

        LoginProxyVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                uri, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), LoginProxyVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            requestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            requestService.failResponseLog(requestLogVo, invalidException);
            throw new InvalidVendorResponseException();
        }

        return responseVo;
    }
}
