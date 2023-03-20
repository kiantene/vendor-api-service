package com.nextgen.gameaggregator.vendor.jili.api.sessionbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionBetDto implements BetData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String reqId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String token;
    @NotBlank
    private String currency;
    @PositiveOrZero
    @NotNull
    private Integer game;
    @Positive
    @NotNull
    private BigInteger round;
    @Positive
    @NotNull
    @Range(min=0, max=2147483647)
    private BigInteger wagersTime;
    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 4)
    private BigDecimal betAmount;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal winloseAmount;
    @Positive
    @NotNull
    private BigInteger sessionId;
    @Positive
    @NotNull
    private Integer type;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String userId;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal turnover;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal preserve;
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String platform;
    @Digits(integer = 12, fraction = 4)
    private BigDecimal sessionTotalBet;
    @Positive
    private Integer statementType;

    @Override
    public String getExternalTransactionId() { return this.reqId; }
    @Override
    public BigDecimal getAmount() { return this.betAmount; }
    @Override
    public String getRoundId() { return String.valueOf(this.round); }
    @Override
    public String getGameId() { return String.valueOf(this.game); }
    @Override
    public Long getTimestamp() {
        Long timestamp = this.wagersTime.longValueExact();
        if(String.valueOf(Math.abs(timestamp)).length() > 10){
            return timestamp;
        }
        return timestamp * 1000;
    }
}
