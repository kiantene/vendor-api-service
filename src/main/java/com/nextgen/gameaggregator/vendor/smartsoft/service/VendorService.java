package com.nextgen.gameaggregator.vendor.smartsoft.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {
    private String agentId;
    private String agentKey;

    public static String md5Generator(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static String signatureGenerator(String secretKey, String requestMethod, String requestPayload) {
        return md5Generator(secretKey + "|" + requestMethod + "|" + requestPayload);
    }

}
