package com.nextgen.gameaggregator.vendor.iloveu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataDto {

    @JsonProperty("playerCode")
    private String playerCode;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("loginUrl")
    private String loginUrl;
}
