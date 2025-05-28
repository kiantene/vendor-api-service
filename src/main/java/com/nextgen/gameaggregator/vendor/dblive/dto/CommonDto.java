package com.nextgen.gameaggregator.vendor.dblive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @NotBlank
    private String merchantCode;
    @NotBlank
    private String params;
    @NotBlank
    private String signature;
    @NotNull
    private Long timestamp;
}
