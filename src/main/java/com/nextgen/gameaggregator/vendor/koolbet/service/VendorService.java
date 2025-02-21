package com.nextgen.gameaggregator.vendor.koolbet.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Formats;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class VendorService extends BaseVendorService {

    public static String generateKey(MultiValueMap<String, String> params, String agentId, String agentKey) {
        try {

            String dayText = ZonedDateTime.now(ZoneId.of("UTC-4"))
                    .format(DateTimeFormatter.ofPattern("yyMMd"));

            // Generate final key
            return generateRandomText() + md5(buildParamText(params, agentId) + md5(dayText + agentId + agentKey))
                    + generateRandomText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String md5(String input) {
        return DigestUtils.md5Hex(input);
    }

    private static String generateRandomText() {
        return RandomStringUtils.randomAlphanumeric(Formats.RANDOM_STRING_LENGTH);
    }

    private static String buildParamText(MultiValueMap<String, String> params, String agentId) {
        StringBuilder paramText = new StringBuilder();

        for (String key : params.keySet()) {
            String value = params.get(key).get(0);
            paramText.append(key);
            paramText.append("=");
            paramText.append(value);
            paramText.append("&");
        }

        return paramText.append("AgentId=").append(agentId).toString();
    }
}
