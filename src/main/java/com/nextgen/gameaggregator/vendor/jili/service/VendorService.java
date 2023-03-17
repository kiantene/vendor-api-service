package com.nextgen.gameaggregator.vendor.jili.service;

import com.nextgen.gameaggregator.vendor.jili.constant.Formats;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Service
@Slf4j
@Data
public class VendorService {

    private String agentId;
    private String agentKey;

    public static String dateGenerator(String dateFormat, String timeZone) {

        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone));

        return sdf.format(new Date());
    }
    public static String urlQueryStringGenerator(MultiValueMap<String, String> params) {

        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            List<String> values = params.get(key);
            for (String value : values) {
                sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                sb.append("&");
            }
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
        return md5Generator(dateGenerator(Formats.DATE_FORMAT, Formats.TIME_ZONE)+this.agentId+this.agentKey);
    }

    public String keyGenerator(MultiValueMap<String, String> params) {
        return randomStringGenerator(Formats.RANDOM_STRING_LENGTH)+md5Generator(urlQueryStringGenerator(params)+gKeyGenerator())+randomStringGenerator(Formats.RANDOM_STRING_LENGTH);
    }
}
