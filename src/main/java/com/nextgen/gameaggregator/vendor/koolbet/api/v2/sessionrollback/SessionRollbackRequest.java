package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionrollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionRollbackRequest {

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String reqId;

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotNull
    @Positive
    private Integer game;

    @NotNull
    @Positive
    private BigInteger round;

    @NotNull
    @PositiveOrZero
    private BigDecimal betAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal winloseAmount;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max = 50)
    private String userId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String token;

    @NotNull
    @Positive
    private BigInteger sessionId;

    @NotNull
    @Positive
    private Integer type;

    @AssertTrue(message = "Type must be 1")
    public boolean isTypeValid() {
        return type != null && type == 1;
    }

}
