package com.nextgen.gameaggregator.vendor.mg.api.betresult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaDataDto {
    private Boolean isFreeGame;
    private ProgressiveDto progressive;
}
