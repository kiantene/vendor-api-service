package com.nextgen.gameaggregator.vendor.jili.service;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Data
public class VendorService {

    private String agentId;
    private String agentKey;
    private static String dateFormat = "yyMMd";
    private static int randomStringLength = 6;

    public static String dateGenerator(String dateFormat) throws Exception{

        String date = (new SimpleDateFormat(dateFormat)).format(new Date());

        return date;
    }
    public static String urlQueryStringGenerator(MultiValueMap<String, String> params) throws Exception {

        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            List<String> values = params.get(key);
            for (String value : values) {
                sb.append(URLEncoder.encode(key, "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(value, "UTF-8"));
                sb.append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1); // remove the last '&'
        String URL_Query =  sb.toString();

        return URL_Query;
    }
    public static String md5Generator(String input) throws Exception {
        return DigestUtils.md5Hex(input);
    }

    public static String randomStringGenerator(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public String gKeyGenerator() throws Exception {
        return md5Generator(dateGenerator(VendorService.dateFormat)+this.agentId+this.agentKey);
    }

    public String keyGenerator(MultiValueMap<String, String> params) throws Exception {
        return randomStringGenerator(VendorService.randomStringLength)+md5Generator(urlQueryStringGenerator(params)+gKeyGenerator())+randomStringGenerator(VendorService.randomStringLength);
    }
}
