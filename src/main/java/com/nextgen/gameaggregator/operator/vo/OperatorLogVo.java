package com.nextgen.gameaggregator.operator.vo;

import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
public class OperatorLogVo {
    String endpoint;
    String callbackUrl;
    String signature;
    Object requestObject;

    ResponseEntity responseEntity;
    String profilesActive;
}
