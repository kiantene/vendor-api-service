package com.nextgen.gameaggregator.vendor.epicwin.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    @NotBlank
    @Size(min = 1, max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("OperatorId")
    private String operatorId;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    @JsonProperty("RequestDateTime")
    private String requestDateTime;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("Signature")
    private String signature;

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("PlayerId")
    private String playerId;

    @NotBlank
    @Size(min = 1, max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("Currency")
    private String currency;

    @NotBlank
    @Size(min = 1, max = 500)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("AuthToken")
    private String authToken;
}
