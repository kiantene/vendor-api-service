package com.nextgen.gameaggregator.vendor.crystal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorLineService;
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


//    public void validate(String apiIdDto, String apiKeyDto, String hashKeyDto, GameSession gameSession)
//            throws AuthenticationException, CredentialNotFoundException, InvalidPlayerException {
//
//        String apiId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_ID);
//        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
//
//
//        ValidationUtils.isEquals(apiId, apiIdDto, AuthenticationException::new);
//        ValidationUtils.isEquals(apiKey, apiKeyDto, AuthenticationException::new);
//        ValidationUtils.isEquals(hashedKey, hashKeyDto, InvalidPlayerException::new);
//    }

    public void doCompareSignature(HttpServletRequest request, HttpRequestLog httpRequestLog, GameSession gameSession)
            throws AuthenticationException, CredentialNotFoundException {

        String secretkey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        // 1. 获取请求头中的签名
        String signatureHeader = request.getHeader(WalletServiceEndpoints.HEADER_SIGNATURE);
        if (signatureHeader == null || signatureHeader.isEmpty()) {
            throw new AuthenticationException("Missing signature header");
        }

        // 2. 获取请求体
        String body = httpRequestLog.getRequestBody();
        if (body == null || body.isEmpty()) {
            throw new AuthenticationException("Empty request body");
        }

        // 3. 将body转换为紧凑JSON格式（移除所有空格）
        String compactJsonBody = convertToJson(body);

        // 4. 使用HMAC-SHA256加密
        String computedSignature = hashHMACSha256(compactJsonBody, secretkey);

        // 5. 比较签名
        if (!computedSignature.equalsIgnoreCase(signatureHeader)) {
            throw new AuthenticationException("Invalid signature");
        }
    }
}

