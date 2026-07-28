package com.nextgen.gameaggregator.vendor.groove.util;

import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.service.BaseVendorService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
public class VendorUtil extends BaseVendorService {

    public static String extractTokenFromSessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        String[] parts = sessionId.split("_", 2);
        return parts.length > 1 ? parts[1] : parts[0];
    }

    public static String generateSignature(String queryString, String securityKey) throws NoSuchAlgorithmException, InvalidKeyException {
        if (queryString == null || queryString.isBlank()) {
            return "";
        }

        // Extracted parsing logic simplifies cognitive nested depth here
        Map<String, String> sortedParams = parseAndSortQueryString(queryString);

        // Concatenate values
        StringBuilder concatenated = new StringBuilder();
        for (String value : sortedParams.values()) {
            concatenated.append(value);
        }

        return calculateHmacSha256Hex(concatenated.toString(), securityKey);
    }

    /**
     * Helper method to parse query params. Optimized to use at most one continue statement.
     */
    private static Map<String, String> parseAndSortQueryString(String queryString) {
        // Use a TreeMap directly to guarantee alphabetical key sorting order automatically
        Map<String, String> sortedParams = new TreeMap<>();

        for (String param : queryString.split("&")) {
            if (!param.isBlank()) {
                addParamToMap(param, sortedParams);
            }
        }

        return sortedParams;
    }

    /**
     * Extracts individual parameters, cleanses them, and places them inside the sorted map.
     */
    private static void addParamToMap(String param, Map<String, String> map) {
        int eqIdx = param.indexOf("=");
        String rawKey = eqIdx > 0 ? param.substring(0, eqIdx) : param;
        String rawValue = (eqIdx > 0 && param.length() > eqIdx + 1) ? param.substring(eqIdx + 1) : "";

        String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
        String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);

        // Skip 'request' parameter
        if ("request".equals(key)) {
            return;
        }

        // Handle Groove's specific 'nogsgameid' alias constraint for sorting
        String finalKey = "nogsgameid".equals(key) ? "gameid" : key;

        map.put(finalKey, value);
    }

    /**
     * Isolates standard crypto computation algorithms to maintain clean method profiles.
     */
    private static String calculateHmacSha256Hex(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        // Generate HMAC-SHA256
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static BigDecimal formatBalance(BigDecimal balance) {
        return Objects.requireNonNullElse(balance, BigDecimal.ZERO).setScale(2, RoundingMode.DOWN);
    }

    public static String getGameCode(IBetDetailUrlInfo iBetDetailUrlInfo) {
        String fullGameCode = iBetDetailUrlInfo.getGameCode();
        String gameCode = "";
        if (fullGameCode != null) {
            int underscoreIndex = fullGameCode.indexOf("_");
            gameCode = (underscoreIndex != -1) ? fullGameCode.substring(underscoreIndex + 1) : fullGameCode;
        }
        return gameCode;
    }

    public static String getDateTime(Long time) {
        LocalDate date = Instant.ofEpochMilli(time)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        return date.toString();
    }
}