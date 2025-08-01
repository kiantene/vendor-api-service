package com.nextgen.gameaggregator.vendor.inout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @NotNull
    private String action;

    private CommonDataDto commonDataDto;
}
