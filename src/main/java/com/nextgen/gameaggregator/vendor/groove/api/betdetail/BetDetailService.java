package com.nextgen.gameaggregator.vendor.groove.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.groove.constant.Credentials;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BetDetailService implements BetDetailUrl {

    private static final String REDACTED = "***REDACTED***";

    @Autowired
    private RequestService requestService;

    @Autowired
    private WebClient grooveWebClient;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    // credential value
    private String email;
    private String password;
    private String gameURL;
    private String operatorID;
    private static final String VERSION = "1.0";
    private static final String JWT_AUTH = "jwt-auth";


    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {
        this.email = ValidationUtils.validateCredential(credentials.get(Credentials.EMAIL));
        this.password = ValidationUtils.validateCredential(credentials.get(Credentials.PASS));
        this.gameURL = ValidationUtils.validateCredential(credentials.get(Credentials.GAME_URL));
        this.operatorID = ValidationUtils.validateCredential(credentials.get(Credentials.OPERATOR_ID));
        return new LinkedMultiValueMap<>();
    }

    @Override
    public GrooveBetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        String jwtToken = this.loginUser();
        String fromDate = VendorUtil.getDateTime(iBetDetailUrlInfo.getVendorBetTime());
        String toDate = VendorUtil.getDateTime(iBetDetailUrlInfo.getVendorSettleTime());

        GrooveBetDetailUrlVo responseVo;
        MultiValueMap<String, String> betDetailsForm = new LinkedMultiValueMap<>();
        betDetailsForm.add("from", fromDate);
        betDetailsForm.add("to", toDate);

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = grooveWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(URI.create(this.gameURL).getScheme())
                        .host(URI.create(this.gameURL).getHost())
                        .port(URI.create(this.gameURL).getPort())
                        .path(EndPoints.TRANSACTION + "/" + VERSION + EndPoints.BET_DETAILS + this.operatorID)
                        .pathSegment(VendorUtil.getGameCode(iBetDetailUrlInfo))
                        .pathSegment(iBetDetailUrlInfo.getExternalRoundId())
                        .queryParam("from", fromDate)
                        .queryParam("to", toDate)
                        .build())
                .header(JWT_AUTH, jwtToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retryWhen(Retry.backoff(EndPoints.RETRY, Duration.ofMillis(200))
                        .filter(throwable -> throwable instanceof ConnectException
                                || throwable instanceof TimeoutException))
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.BET_DETAILS, this.gameURL, betDetailsForm, redactSensitiveHeaders(apiResponse), new LinkedMultiValueMap<>(), startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);
        GameSession gameSession = new GameSession();

        try {
            if (apiResponse == null) {
                throw new InvalidVendorResponseException("Response is null");
            }
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), GrooveBetDetailUrlVo.class);
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);

            RequestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException invalidException) {
            log.error("Failed to fetch bet detail from Groove", invalidException);
            requestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();

        } catch (Exception exception) {
            log.error("Unexpected error fetching bet detail from Groove", exception);
            requestService.failResponseLog(requestLogVo, exception, gameSession);
            throw new InvalidVendorResponseException();

        }
        return responseVo;
    }


    private String loginUser() throws InvalidVendorResponseException {

        Map<String, String> loginPayload = new LinkedHashMap<>();
        loginPayload.put("email", this.email);
        loginPayload.put("password", this.password);

        String jsonBody = new Gson().toJson(loginPayload);

        String fullUrl = this.gameURL + "/" + VERSION + EndPoints.LOGIN;
        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = grooveWebClient
                .post()
                .uri(fullUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retryWhen(Retry.backoff(EndPoints.RETRY, Duration.ofMillis(200))
                        .filter(throwable -> throwable instanceof ConnectException
                                || throwable instanceof TimeoutException))
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();

        Map<String, String> redactedPayload = new LinkedHashMap<>(loginPayload);
        redactedPayload.put("password", REDACTED);
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.LOGIN, this.gameURL, redactedPayload, redactSensitiveHeaders(apiResponse), new LinkedMultiValueMap<>(), startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);
        GameSession gameSession = new GameSession();

        try {
            if (apiResponse == null) {
                throw new InvalidVendorResponseException("Response is null");
            }
            requestService.validateVendorHttpStatusResponse(apiResponse);

            String jwtAuthToken = apiResponse.getHeaders().getFirst(JWT_AUTH);

            if (jwtAuthToken == null || jwtAuthToken.isBlank()) {
                throw new InvalidVendorResponseException("Missing jwt-auth header");
            }
            GrooveLoginResponseVo loginVo = new GrooveLoginResponseVo();
            loginVo.setJwtToken(jwtAuthToken);
            requestService.validateResponse(loginVo);
            requestService.successResponseLog(requestLogVo);

            return loginVo.getJwtToken();

        } catch (HttpResponseStatusCodeException | JsonSyntaxException invalidException) {
            log.error("Failed to login to Groove", invalidException);
            requestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();

        } catch (Exception exception) {
            log.error("Unexpected error logging in to Groove", exception);
            requestService.failResponseLog(requestLogVo, exception, gameSession);
            throw new InvalidVendorResponseException();
        }
    }

    private ResponseEntity<String> redactSensitiveHeaders(ResponseEntity<String> original) {
        if (original == null) {
            return null;
        }

        HttpHeaders redactedHeaders = new HttpHeaders();
        redactedHeaders.addAll(original.getHeaders());
        if (redactedHeaders.containsKey(JWT_AUTH)) {
            redactedHeaders.set(JWT_AUTH, REDACTED);
        }

        return new ResponseEntity<>(original.getBody(), redactedHeaders, original.getStatusCode());
    }
}