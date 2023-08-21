package com.nextgen.gameaggregator.vendor.evolution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableDto {
    private String id;
    private String vid;
}