package com.nextgen.gameaggregator.vendor.bng.api.action;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    @JsonProperty("name")
    private String name;
}
