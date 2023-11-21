package com.nextgen.gameaggregator.vendor.spribe.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import com.nextgen.gameaggregator.service.BaseVendorService;

@Service
public class VendorService extends BaseVendorService {
    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }

    public String toQueryString(MultiValueMap<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                queryString.append("=");
                queryString.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return queryString.toString();
    }
}