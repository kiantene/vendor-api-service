package com.nextgen.gameaggregator.vendor.mg.api.betdetail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDetailDto {
    private int utcOffset;
    private String betId;
    private String langCode;
}
