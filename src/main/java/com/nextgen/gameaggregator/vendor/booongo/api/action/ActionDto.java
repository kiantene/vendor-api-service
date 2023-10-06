package com.nextgen.gameaggregator.vendor.booongo.api.action;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    @NotBlank
    @JsonProperty("name")
    private String name;

    @JsonProperty("args")
    private ActionArgsDTO args;
}
