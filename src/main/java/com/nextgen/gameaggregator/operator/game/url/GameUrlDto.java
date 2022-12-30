package com.nextgen.gameaggregator.operator.game.url;

import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;

@Data
public class GameUrlDto {

    @NotBlank
    @Size(min = 36, max = 36)
    private String traceId;

    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String username;

    @NotNull
    @Digits(integer = 5, fraction = 0)
    // TODO: to be changed to string type game code instead
    private Integer gameId;

    @NotBlank
    @Size(min = 2, max = 2)
    private String language;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String platform;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;
}
