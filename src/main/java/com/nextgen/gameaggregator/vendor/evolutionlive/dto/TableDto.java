package com.nextgen.gameaggregator.vendor.evolutionlive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableDto {
    @NotNull
    private String id;
    private String vid;
}