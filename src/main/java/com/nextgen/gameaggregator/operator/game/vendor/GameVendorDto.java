package com.nextgen.gameaggregator.operator.game.vendor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameVendorDto {
    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "2 alphanumeric")
    @Size(min = 2, max = 2, message = " 2 alphanumeric only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "2 alphanumeric only") // Only alphanumeric allowed
    private String displayLanguage = "en";
}
