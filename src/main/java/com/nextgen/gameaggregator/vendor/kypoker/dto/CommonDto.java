package com.nextgen.gameaggregator.vendor.kypoker.dto;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.Data;

@Data
public class CommonDto {
    private String agent;
    private String timestamp;
    private String params;
    private String key;
    private HttpRequestLog httpRequestLog;
}
