package com.nextgen.gameaggregator.vendor.kypoker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommonDto {
    private String agent;
    private String timestamp;
    private String param;
    private String key;
    private HttpRequestLog httpRequestLog;
}
