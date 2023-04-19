package com.nextgen.gameaggregator.util;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

@Data
public class RequestLogVo {
    String endpoint;
    String callbackUrl;
    MultiValueMap<String, String> requestHeaders;
    Object requestObject;
    ResponseEntity responseEntity;
    String profilesActive;
    String packageName;

    Long startTime;
    Long endTime;

}
