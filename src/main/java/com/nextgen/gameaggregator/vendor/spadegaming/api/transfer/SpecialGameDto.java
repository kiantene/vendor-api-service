package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecialGameDto {
    @Size(max = 20)
    private String type;

    private Integer count;

    private Integer sequence;
}
