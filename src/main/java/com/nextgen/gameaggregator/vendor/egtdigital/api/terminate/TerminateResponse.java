package com.nextgen.gameaggregator.vendor.egtdigital.api.terminate;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@Setter
public class TerminateResponse {
    private String statusCode;
}
