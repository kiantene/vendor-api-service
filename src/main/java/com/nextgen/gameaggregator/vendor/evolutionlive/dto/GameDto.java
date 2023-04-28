package com.nextgen.gameaggregator.vendor.evolutionlive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDto {
    private String id;
    private String type;
    private DetailsDto details;
}