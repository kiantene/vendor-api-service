package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionBetAndResultRequest {
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

    private List<String> betOrder;

    @NotNull
    @PositiveOrZero
    private Long wagersTime;

    @NotNull
    @Positive
    private BigDecimal betAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal winloseAmount;

    @NotNull
    @PositiveOrZero
    private BigInteger sessionId;

    @NotNull
    @Positive
    private Integer type;

    @NotNull
    @PositiveOrZero
    private BigDecimal preserve;

    private Boolean isOver;

    private String userId;

    private String username;

    public Boolean getIsOver() {
        return isOver != null ? isOver : Boolean.TRUE;
    }

    @AssertTrue(message = "Type must be one of: 1, 2")
    public boolean isTypeValid() {
        return type != null && Set.of(1, 2).contains(type);
    }

}
