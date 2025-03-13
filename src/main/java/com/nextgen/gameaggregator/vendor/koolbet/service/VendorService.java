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
import java.util.stream.Collectors;


@Service
public class VendorService extends BaseVendorService {

    public static String generateKey(MultiValueMap<String, String> params, String agentId, String agentKey) {
        String dayText = getDate();
        String paramText = buildParamText(params, agentId);
        String authKey = md5(dayText + agentId + agentKey);
        String md5Result = md5(paramText + authKey);

        // Generate final key
        return generateRandomText() + md5Result + generateRandomText();

    }

    private static String md5(String input) {
        return DigestUtils.md5Hex(input);
    }

    private static String generateRandomText() {
        return RandomStringUtils.randomAlphanumeric(Formats.RANDOM_STRING_LENGTH);
    }

    private static String getDate() {
        return ZonedDateTime.now(ZoneId.of(Formats.TIME_ZONE))
                .format(DateTimeFormatter.ofPattern(Formats.DATE_FORMAT));
    }

    private static String buildParamText(MultiValueMap<String, String> params, String agentId) {

        return params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue().get(0))
                .collect(Collectors.joining("&")) + "&AgentId=" + agentId;

    }
}
