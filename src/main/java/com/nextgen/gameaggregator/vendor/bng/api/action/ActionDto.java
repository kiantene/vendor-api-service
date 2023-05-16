package com.nextgen.gameaggregator.vendor.bng.api.action;

import jakarta.validation.constraints.NotNull;

import lombok.Data;
import org.checkerframework.checker.index.qual.Positive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    @NotNull
    @Positive
    @JsonProperty("name")
    private String name;
}
