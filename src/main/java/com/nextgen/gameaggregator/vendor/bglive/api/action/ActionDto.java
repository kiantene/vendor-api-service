package com.nextgen.gameaggregator.vendor.bglive.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    @JsonProperty("method")
    private String method;
}
