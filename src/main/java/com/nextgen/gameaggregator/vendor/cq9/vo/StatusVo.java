package com.nextgen.gameaggregator.vendor.cq9.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusVo {
    private String code = "0"; // This variable will be 0 when there is no error.
    private String message; // Success
    @JsonProperty("datetime")
    private String dateTime; // DateTime format = RFC3339
}
