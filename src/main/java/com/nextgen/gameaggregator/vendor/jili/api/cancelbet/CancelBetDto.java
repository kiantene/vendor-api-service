package com.nextgen.gameaggregator.vendor.jili.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String reqId;
    @NotBlank
    private String currency;
    @PositiveOrZero
    @NotNull
    private Integer game;
    @Positive
    @NotNull
    private BigInteger round;
    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 4)
    private BigDecimal betAmount;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal winloseAmount;
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String userId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String token;

    public String getExternalTransactionId() {
        return String.valueOf(this.round);
    }
}
