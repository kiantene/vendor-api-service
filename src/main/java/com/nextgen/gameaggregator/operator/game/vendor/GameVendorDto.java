package com.nextgen.gameaggregator.operator.game.vendor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameVendorDto {
    @NotBlank
    @Size(min = 36, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric allowed
    private String traceId;

    @NotBlank
    @Size(min = 2, max = 20)
    private String displayLanguage = "en";
}
