package com.nextgen.gameaggregator.vendor.jdb.api.action;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import jakarta.validation.constraints.NotNull;

import org.checkerframework.checker.index.qual.Positive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
