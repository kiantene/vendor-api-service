package com.nextgen.gameaggregator.vendor.koolbet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String reqId;

    @NotBlank
    @Size(max = 255)
    private String token;
}
