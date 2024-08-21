package com.nextgen.gameaggregator.vendor.jili.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto implements RollbackData {
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

    /*
    @Positive
    @NotNull
    @Range(min = 0, max = 2147483647)
    private BigInteger wagersTime;
     */

    @Override
    public String getRollbackId() {
        return String.valueOf(this.round);
    }

    @Override
    public Long getVendorSettledTime() {
        // return getTimestamp();
        return null;
    }

    @Override
    public String getRoundId() {
        return null;
    }

    /*
    private Long getTimestamp() {
        long timestamp = this.getWagersTime().longValueExact();
        if (String.valueOf(Math.abs(timestamp)).length() > 10) {
            return timestamp;
        }
        return timestamp * 1000;
    }
     */
}
