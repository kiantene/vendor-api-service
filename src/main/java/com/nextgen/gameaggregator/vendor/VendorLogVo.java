package com.nextgen.gameaggregator.vendor;

import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
public class VendorLogVo {
    String endpoint;
    String callbackUrl;
    Object requestObject;
    ResponseEntity responseEntity;
    String profilesActive;
}
