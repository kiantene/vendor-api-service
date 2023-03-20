package com.nextgen.gameaggregator.vendor.jili.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;
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
    @Range(min = 0)
    @NotNull
    private BigDecimal betAmount;
    @NotNull
    private BigDecimal winloseAmount;
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String userId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String token;
}
