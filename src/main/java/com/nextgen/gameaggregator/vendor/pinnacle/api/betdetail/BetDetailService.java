package com.nextgen.gameaggregator.vendor.pinnacle.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import com.nextgen.gameaggregator.operator.transactions.detail.SportBetDetail;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BetDetailService implements SportBetDetail<BetDetailUrlVo> {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        // Endpoints.MY_BETS_FULL
        formData.add("loginId", iBetDetailUrlInfo.getVendorUsername());
        formData.add("locale", vendorLanguageCode.getLanguageCode());

        // Endpoints.REPORT_ALL_WAGERS
//        formData.add("userCode1", iBetDetailUrlInfo.getVendorUsername());
//        formData.add("locale", vendorLanguageCode.getLanguageCode());
//        formData.add("wagerIds", iBetDetailUrlInfo.getExternalTransactionId());

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        String apiUrl = Optional.ofNullable(credentials.get(Credentials.API_URL)).orElseThrow(InvalidVendorLineException::new);
        String agentCode = Optional.ofNullable(credentials.get(Credentials.AGENT_CODE)).orElseThrow(InvalidVendorLineException::new);
        String agentKey = Optional.ofNullable(credentials.get(Credentials.AGENT_KEY)).orElseThrow(InvalidVendorLineException::new);
        String secretKey = Optional.ofNullable(credentials.get(Credentials.SECRET_KEY)).orElseThrow(InvalidVendorLineException::new);

        BetDetailVo responseVo = new BetDetailVo();
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add("userCode", agentCode);
        headerMap.add("token", VendorService.generateToken(agentCode, agentKey, secretKey));

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(Endpoints.MY_BETS_FULL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .get()
                .uri(uri)
                .headers(headers -> headers.addAll(headerMap))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(Endpoints.RETRY)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                "", uri.toString(), formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(Objects.requireNonNull(apiResponse));
            responseVo = new Gson().fromJson(apiResponse.getBody(), BetDetailVo.class);


            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            GameSession gameSession = new GameSession();
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }

        return responseVo;
    }

}
