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
    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "min 2 characters")
    @Size(min = 2, max = 20, message = "min 2 characters")
    private String displayLanguage = "en";
}
