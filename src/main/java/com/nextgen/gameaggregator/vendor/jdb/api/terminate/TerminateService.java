package com.nextgen.gameaggregator.vendor.jdb.api.terminate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.jdb.constant.Actions;
import com.nextgen.gameaggregator.vendor.jdb.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class TerminateService {

    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public void terminate(GameSession gameSession, HttpServletRequest request, VendorLine vendorLine) throws JsonProcessingException, InvalidFormatException, CredentialNotFoundException, InvalidVendorLineException, InvalidVendorResponseException, DisabledVendorLineException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        //prepare the credential params
        String apiUrl = vendorService.getCredentialWithKey(vendorLine, Credentials.API_SERVER);
        String parent = vendorService.getCredentialWithKey(vendorLine, Credentials.PARENT);
        String dc = vendorService.getCredentialWithKey(vendorLine, Credentials.DC);
        String key = vendorService.getCredentialWithKey(vendorLine, Credentials.KEY);
        String iv = vendorService.getCredentialWithKey(vendorLine, Credentials.IV);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        CommonVo responseVo = new CommonVo();

        //prepare for content encryption
        TerminateDto dto = new TerminateDto();
        dto.setAction(Actions.TERMINATE_PLAYER);
        dto.setTs(System.currentTimeMillis());
        dto.setParent(parent);
        dto.setUid(gameSession.getVendorPlayerUsername());

        //build vendor url to call
//        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
//                .queryParams(requestBody)
//                .build()
//                .encode()
//                .toUri();

        //call to vendor
        RequestLogVo requestLogVo = new RequestLogVo();
        try {

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String x = VendorService.encrypt(ow.writeValueAsString(dto), key, iv);

            requestBody.add("dc", dc);
            requestBody.add("x", x);

            httpRequestLog.setRequestBody(requestBody.toString());

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParams(requestBody)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .onErrorResume(TimeoutException.class, e -> {
                        log.error("Failed to fetch data from {}: {}", apiUrl, e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching data from " + apiUrl));
                    })
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                    .block();
            long endTime = System.currentTimeMillis();

            requestLogVo = requestService.createRequestLogVo(
                    "", apiUrl, requestBody, apiResponse, null, startTime, endTime,
                    this.getClass().getPackage().getName(), profilesActive);

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), CommonVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException |
                 InvalidVendorResponseException | NullPointerException | IllegalStateException invalidException) {
            gameSession = new GameSession();
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        } catch (Exception exception) {
            exception.printStackTrace();
            RequestService.failResponseLog(requestLogVo, exception, gameSession);
            throw new InvalidVendorResponseException();
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

    }
}
