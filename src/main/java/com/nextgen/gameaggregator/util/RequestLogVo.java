package com.nextgen.gameaggregator.util;

import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
public class RequestLogVo {
    String endpoint;
    String callbackUrl;
    Object requestObject;
    ResponseEntity responseEntity;
    String profilesActive;
    String packageName;
}
