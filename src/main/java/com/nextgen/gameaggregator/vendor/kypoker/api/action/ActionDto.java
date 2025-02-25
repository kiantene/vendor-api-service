package com.nextgen.gameaggregator.vendor.kypoker.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    @NotNull
    @Positive
    @JsonProperty("action")
    private Integer action;
    private String params;
    private HttpRequestLog httpRequestLog;
}
