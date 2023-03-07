package com.nextgen.gameaggregator.vendor.facai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @NotBlank
    @JsonProperty("AgentCode")
    private String agentCode;

    @NotBlank
    @JsonProperty("Currency")
    private String currency;

    @NotBlank
    @JsonProperty("Params")
    private String params;

    @NotBlank
    @JsonProperty("Sign")
    private String sign;
}
