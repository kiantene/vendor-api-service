package com.nextgen.gameaggregator.vendor.mg.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mg.constant.Credentials;

@Service
public class VendorTokenService {
    @Autowired
    private VendorLineService vendorLineService;
    
    public String getToken(Integer vendorLineId) {
        String accessToken = "";
        try {
            String clientId = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AGENT_CODE);
            String clientSecret = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.API_SECRET);
            String tokenUrl = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.TOKEN_URL);
            
            // Create the request body with client_id, client_secret and grant_type as key-value pairs
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("grant_type", "client_credentials");

            // Set headers for the POST request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            WebClient webClient = WebClient.create();

            // Make the POST request and get the response
            String responseBody = webClient
                .post()
                .uri(tokenUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // Extract the access token from the JSON response string
            JSONObject responseJson = new JSONObject(responseBody);
            accessToken = responseJson.getString("access_token");
        } catch (CredentialNotFoundException e) {
            e.printStackTrace();
        }

        return accessToken;
    }
}
