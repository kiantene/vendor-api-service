package com.nextgen.gameaggregator.vendor.koolbet.api.v2.betandresult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetAndResultRequest {
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String reqId;

    @NotBlank
    @Size(max = 255)
    private String token;

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotNull
    @Digits(integer = 20, fraction = 0)
    private BigDecimal game;

    @NotNull
    @PositiveOrZero
    private BigInteger round;

    @NotNull
    @Positive
    private Long wagersTime;

    @NotNull
    @Positive
    private BigDecimal betAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal winloseAmount;

    private String username;
}
