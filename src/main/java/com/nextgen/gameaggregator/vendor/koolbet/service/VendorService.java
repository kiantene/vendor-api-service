package com.nextgen.gameaggregator.vendor.koolbet.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {
    private static final String AGENT_ID = "kb469zf_oneapistg";
    private static final String AGENT_KEY = "6a37bbb11a222f63d2ce7a57d2c180eef1a23bc8";

    // Characters for random text generation
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateKey(Map<String, String> params, String agentId, String agentKey) {
        try {
            // Generate param_text from parameters
            String paramText = buildParamText(params);

            String dayText = ZonedDateTime.now(ZoneId.of("UTC-4"))
                    .format(DateTimeFormatter.ofPattern("yyMMdd"));
            // Generate auth_key
            String authKey = md5(dayText + AGENT_ID + AGENT_KEY);

            // Generate random texts
            String randomFrontText = generateRandomText();
            String randomEndText = generateRandomText();

            // Generate final key
            String finalKey = randomFrontText + md5(paramText + authKey) + randomEndText;

            log.info("paramText: {}", paramText);
            log.info("dayText: {}", dayText);
            log.info("authKey: {}", authKey);
            log.info("randomFrontText: {}", randomFrontText);
            log.info("randomEndText: {}", randomEndText);
            return finalKey;
        } catch (Exception e) {
            throw new RuntimeException("Error generating key", e);
        }
    }

    private static String md5(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String generateRandomText() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(CHARS.length());
            sb.append(CHARS.charAt(index));
        }
        return sb.toString();
    }

    private static String buildParamText(Map<String, String> params) {
        StringBuilder paramText = new StringBuilder();

        // Add all parameters in the order they were inserted
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (paramText.length() > 0) {
                paramText.append("&");
            }
            paramText.append(entry.getKey()).append("=").append(entry.getValue());
        }

        // Add AgentId at the end
        if (paramText.length() > 0) {
            paramText.append("&");
        }
        paramText.append("AgentId=").append(AGENT_ID);

        return paramText.toString();
    }
}
