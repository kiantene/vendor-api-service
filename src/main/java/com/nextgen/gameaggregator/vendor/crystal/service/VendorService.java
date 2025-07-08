package com.nextgen.gameaggregator.vendor.crystal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.crystal.constant.Credentials;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
@Setter
@Service
public class VendorService extends BaseVendorService {

    private final VendorLineService vendorLineService;

    public VendorService(VendorLineService vendorLineService) {
        this.vendorLineService = vendorLineService;
    }

    public static String convertToJson(String jsonBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object jsonObject = mapper.readValue(jsonBody, Object.class);
            return mapper.writeValueAsString(jsonObject).replaceAll("\\s+", "");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
    }

    public static String hashHMACSha256(String data, String secret) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(dataBytes);
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String convertToCompactJson(MultiValueMap<String, String> formData) {
        Map<String, String> dataMap = formData.toSingleValueMap();
        try {
            return new ObjectMapper().writeValueAsString(dataMap)
                    .replaceAll("\\s+", "");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    public void doCompareSignature(HttpServletRequest request, HttpRequestLog httpRequestLog, GameSession gameSession)
            throws AuthenticationException, CredentialNotFoundException {

        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        String operatorCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_CODE);

        String signatureHeader = request.getHeader(WalletServiceEndpoints.HEADER_SIGNATURE);
        String operatorHeader = request.getHeader(WalletServiceEndpoints.HEADER_OPERATOR);

        if (signatureHeader == null || signatureHeader.isEmpty()) {
            throw new AuthenticationException("Missing signature header");
        }

        String body = httpRequestLog.getRequestBody();
        if (body == null || body.isEmpty()) {
            throw new AuthenticationException("Empty request body");
        }
        ValidationUtils.isEquals(operatorHeader, operatorCode, AuthenticationException::new);
        String compactJsonBody = convertToJson(body);
        String computedSignature = hashHMACSha256(compactJsonBody, secretKey);

        if (!computedSignature.equalsIgnoreCase(signatureHeader)) {
            throw new AuthenticationException("Invalid signature");
        }
    }
}

