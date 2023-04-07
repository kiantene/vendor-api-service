package com.nextgen.gameaggregator.operator.game.url;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlDto {

    @NotBlank
    @Size(min = 36, max = 36)
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only UUID allowed
    private String traceId;

    @NotBlank
    @Size(min = 3, max = 20 , message = "min 3 and max 20 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "min 3 and max 20 alphanumeric only") // Only alphanumeric allowed
    private String username;

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "min 3 and max 50 alphanumeric") // Only alphanumeric allowed
    private String gameCode;

    @NotBlank
    @Size(min = 2, max = 2, message = "min 2 characters")
    private String language;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = ValidationUtils.WEB_OR_H5, message = "Platform is not supported.")
    private String platform;

    @NotBlank
    @Size(min = 3, max = 10, message = "min 3 characters")
    private String currency;
}
