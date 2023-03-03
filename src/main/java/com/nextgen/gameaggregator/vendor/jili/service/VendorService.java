package com.nextgen.gameaggregator.vendor.jili.service;

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
public class VendorService {
    @SneakyThrows
    public static String dateFormat(String pattern) {

        String date = (new SimpleDateFormat(pattern)).format(new Date());

        return date;
    }
    public static String urlQueryString(MultiValueMap<String, String> params) throws Exception {

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
    public static String md5(String input) throws Exception {
        return DigestUtils.md5Hex(input);
    }

    public static String randomString(int length){
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
