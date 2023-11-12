package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Action {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Name")
    private String name;
}
