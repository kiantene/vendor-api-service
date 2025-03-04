package com.nextgen.gameaggregator.vendor.smartsoft.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Formats;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {
    private String agentId;
    private String agentKey;

    public static String md5Generator(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static String signGenerator(Map<String, String> credentials, String timeStamp) {
        return md5Generator(credentials.get(Credentials.AGENT_ID) + credentials.get(Credentials.API_KEY) + timeStamp);
    }

    public static String removeLeadingZero(String input) {
        return input.replaceAll(Formats.JSON_LEADING_ZERO, "$1");
    }
}
