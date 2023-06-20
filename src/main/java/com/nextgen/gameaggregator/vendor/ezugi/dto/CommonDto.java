package com.nextgen.gameaggregator.vendor.ezugi.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommonDto {
    @NotNull
    private Integer operatorId;
    @NotBlank
    private String token;
    @NotNull
    private Integer platformId;
    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long timestamp;
}
