package com.nextgen.gameaggregator.vendor.mg.api.updateBalance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProgressiveDto {
    private String type;
}
