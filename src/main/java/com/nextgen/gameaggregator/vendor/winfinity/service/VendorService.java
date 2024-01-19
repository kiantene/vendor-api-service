package com.nextgen.gameaggregator.vendor.winfinity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.winfinity.constant.Credentials;
import com.nextgen.gameaggregator.vendor.winfinity.constant.EndPoints;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private VendorLineService vendorLineService;

    public String getToken(Integer vendorLineId) {

        String accessToken = "";

        try {
            String clientId = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CLIENT_ID);
            String clientSecret = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CLIENT_SECRET);
            String tokenUrl = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.API_URL)
                    + EndPoints.TOKEN;

            String requestBody = "client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=client_credentials";

            // Set headers for the POST request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Make the POST request and get the response
            String responseBody = WebClient.create()
                    .post()
                    .uri(tokenUrl)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Extract the access token from the JSON response string
            JSONObject responseJson = new JSONObject(responseBody);
            accessToken = responseJson.getString("access_token");

        } catch (CredentialNotFoundException e) {
            log.error("Credential not found : " + e.getMessage());

        } catch (Exception e) {
            log.error("Other exception : ", e.getMessage());
        }

        return accessToken;
    }

    public String decodeRequestBody(Integer vendorLineId, String requestBody, String qa,
            HttpRequestLog httpRequestLog) {
        String publicKey = "";
        String decodedString = "";

        try {
            publicKey = (qa == null) ? vendorLineService.getCredentialValueByName(vendorLineId, Credentials.PUBLIC_KEY)
                    : vendorLineService.getCredentialValueByName(vendorLineId, Credentials.PUBLIC_KEY_QA);

            decodedString = this.decodeRsaJwt(requestBody, publicKey);

        } catch (CredentialNotFoundException e) {
            log.error("Credential not found : " + e.getMessage());
        }

        // Add decrypt value into request body
        httpRequestLog.setRequestBody(requestBody + ", Decrypt Value:" + decodedString);

        return decodedString;
    }

    public String decodeRsaJwt(String jwtToken, String publicKey) {
        String decodedRsaJwt = "";

        try {
            String publicKeyWithoutWhitespace = publicKey.replaceAll("\\s", "");
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyWithoutWhitespace);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey rsaPublicKey = keyFactory.generatePublic(spec);

            Jws<Claims> jws = Jwts.parser().setSigningKey(rsaPublicKey).parseClaimsJws(jwtToken);

            // Convert the jws object to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            decodedRsaJwt = objectMapper.writeValueAsString(jws.getBody());

            log.debug("Decoded JSON: " + decodedRsaJwt);

        } catch (JwtException e) {
            log.error("Error decoding JWT token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing public key: " + e.getMessage());
        }

        return decodedRsaJwt;
    }
}
