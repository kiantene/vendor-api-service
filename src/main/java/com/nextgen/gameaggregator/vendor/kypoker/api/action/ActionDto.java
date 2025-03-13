package com.nextgen.gameaggregator.vendor.kypoker.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    Integer s;
    private HttpRequestLog httpRequestLog;

}
