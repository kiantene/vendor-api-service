package com.nextgen.gameaggregator.vendor.evolutionlive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicDto {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 250)
    private String sid; // Player session token

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 20)
    private String userId; // Player ID

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String uuid; // API request ID
}
