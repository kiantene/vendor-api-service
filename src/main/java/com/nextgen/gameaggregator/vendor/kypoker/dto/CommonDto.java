package com.nextgen.gameaggregator.vendor.kypoker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommonDto {

    @NotBlank
    private String agent;

    @NotBlank
    private String timestamp;

    @NotBlank
    private String param;

    @NotBlank
    private String key;

    private HttpRequestLog httpRequestLog;
}
