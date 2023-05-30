package com.nextgen.gameaggregator.vendor.ezugi.dto;

import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CommonDto {
    @NotNull
    private Integer operatorId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max = 250)
    private String token;
    @NotNull
    private Integer platformId;
    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long timestamp;
}
