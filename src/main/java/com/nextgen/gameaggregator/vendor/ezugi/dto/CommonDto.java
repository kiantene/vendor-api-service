package com.nextgen.gameaggregator.vendor.ezugi.dto;

import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CommonDto {
    @NotNull
    private Integer operatorId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 250)
    private String token;
    @NotNull
    private Integer platformId;
    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long timestamp;
}
