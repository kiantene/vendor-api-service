package com.nextgen.gameaggregator.vendor.egtdigital.api.regenerate;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@Setter
public class RegenerateResponse {

    private String statusCode;
    private String defenceCode;

}