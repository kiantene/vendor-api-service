package com.nextgen.gameaggregator.vendor.alizegames.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    // Identifier of the user within the Casino Operator’s system.
    @NotBlank
    // Size checking is done on each Action
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String username;

    // Token of the player
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String token;

    // Currency
    @NotBlank
    private String currency;

    // OperatorId
    @NotBlank
    private String operatorId;

    @NotBlank
    private Long timestamp;
}
