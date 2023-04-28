package com.nextgen.gameaggregator.vendor.evolutionlive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailsDto {
    private TableDto table;
}

