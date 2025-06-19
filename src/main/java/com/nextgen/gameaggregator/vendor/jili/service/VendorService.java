package com.nextgen.gameaggregator.vendor.jili.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.util.DateTimeConversionUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.Formats;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.ZoneId;
import java.util.Objects;

@Service
@Getter
@Setter
public class VendorService extends BaseVendorService {

    private String agentId;
    private String agentKey;
    private Integer operatorTiming;

    public static String urlQueryStringGenerator(MultiValueMap<String, String> params) {

        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            String value = params.get(key).get(0);
            sb.append(key);
            sb.append("=");
            sb.append(value);
            sb.append("&");
        }
        return sb.deleteCharAt(sb.length() - 1).toString(); // remove the last '&'
    }

    public static String md5Generator(String input) {
        return DigestUtils.md5Hex(input);
    }

    public static String randomStringGenerator(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public String gKeyGenerator() {
        String dateStr = DateTimeConversionUtils.fromUnixTimestamp(System.currentTimeMillis(), Formats.DATE_FORMAT, ZoneId.of(Formats.TIME_ZONE));
        return md5Generator(dateStr + this.getAgentId() + this.getAgentKey());
    }

    public String keyGenerator(MultiValueMap<String, String> params) {
        return randomStringGenerator(Formats.RANDOM_STRING_LENGTH) + md5Generator(urlQueryStringGenerator(params) + gKeyGenerator()) + randomStringGenerator(Formats.RANDOM_STRING_LENGTH);
    }

    @Override
    public Integer operatorTimeoutTiming() {
        return Objects.requireNonNullElse(this.operatorTiming, 5000);
    }
}
