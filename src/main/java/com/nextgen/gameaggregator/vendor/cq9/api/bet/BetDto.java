package com.nextgen.gameaggregator.vendor.cq9.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetData {
    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String account;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 1, max = 36)
    private String gamehall;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 1, max = 36)
    private String gamecode;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 1, max = 50)
    private String roundid;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 10)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 1, max = 70)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX)
    private String mtcode;

    @NotBlank
    private String session;

    @Pattern(regexp = "^(web|mobile)$")
    private String platform;

    @NotBlank
    private String eventTime;

    @Override
    public String getExternalTransactionId() {
        return mtcode;
    }

    @Override
    public String getRoundId() {
        return roundid;
    }

    @Override
    public String getGameId() {
        return gamecode;
    }

    @Override
    public Long getTimestamp() {
        Instant instant = Instant.parse(this.getEventTime());
        return instant.getEpochSecond();
    }
}
