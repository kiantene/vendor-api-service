package com.nextgen.gameaggregator.vendor.saba.api.createmember;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.saba.constant.Credentials;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
public class CreateMemberService {
    @Autowired
    private RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public void call(GameSession gameSession, Map<String, String> credentials) throws InvalidVendorResponseException {

        String vendorId = credentials.get(Credentials.VENDOR_ID);
        String operatorId = credentials.get(Credentials.OPERATOR_ID);
        String apiUrl = credentials.get(Credentials.API_URL);
        String oddsType = credentials.get(Credentials.ODDS_TYPE);
        String minTransfer = credentials.get(Credentials.MIN_TRANSFER);
        String maxTransfer = credentials.get(Credentials.MAX_TRANSFER);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("vendor_id", vendorId);
        formData.add("vendor_member_id", gameSession.getVendorPlayerUsername());
        formData.add("operatorid", operatorId);
        formData.add("username", gameSession.getVendorPlayerUsername());
        formData.add("oddstype", oddsType);
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("mintransfer", minTransfer);
        formData.add("maxtransfer", maxTransfer);

        CreateMemberVo responseVo = null;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();

        Long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.CREATE_MEMBER)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .block();

        Long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.GAME_URL, apiUrl, formData, apiResponse, headers, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), CreateMemberVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);

            if (responseVo.getErrorCode().equals(0) || responseVo.getErrorCode().equals(6)) {
                //0 is create new member
                //6 is duplicate vendor_member_id
            } else {
                throw new InvalidVendorResponseException();
            }

            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException |
                 InvalidVendorResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }
    }
}
