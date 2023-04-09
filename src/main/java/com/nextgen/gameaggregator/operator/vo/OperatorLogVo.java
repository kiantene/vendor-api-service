package com.nextgen.gameaggregator.operator.vo;

import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
public class OperatorLogVo {
    String endpoint;
    String callbackUrl;
    Object requestObject;
    ResponseEntity responseEntity;
    String profilesActive;
}
