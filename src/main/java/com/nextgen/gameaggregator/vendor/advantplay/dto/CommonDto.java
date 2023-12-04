package com.nextgen.gameaggregator.vendor.advantplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CommonDto {
    private String timestamp;
    private String seq;
    @JsonProperty("OPToken")
    private String opToken;
    private String brandCode;
    private String siteCode;
    private String gameCode;
}
