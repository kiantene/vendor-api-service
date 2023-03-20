package com.nextgen.gameaggregator.vendor.jdb.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    @JsonProperty("action")
    private String action;
    private String params;
}
