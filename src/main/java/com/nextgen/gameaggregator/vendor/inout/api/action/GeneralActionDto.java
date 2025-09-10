package com.nextgen.gameaggregator.vendor.inout.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneralActionDto {
    private String operator;
}