package com.nextgen.gameaggregator.vendor.jili.api.bet;

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
public class BetDto implements BetData {
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
    private BigInteger wagersTime;
    @Range(min = 0)
    @NotNull
    private BigDecimal betAmount;
    @NotNull
    private BigDecimal winloseAmount;
    private boolean isFreeRound;
    private String userId;
    private BigInteger transactionId;
    private String platform;
    private Integer statementType;
    private Integer gameCategory;

    @Override
    public String getExternalTransactionId() { return this.reqId; }
    @Override
    public BigDecimal getAmount() { return this.betAmount; }
    @Override
    public String getRoundId() { return String.valueOf(this.round); }
    @Override
    public String getGameId() { return String.valueOf(this.game); }
    @Override
    public Long getTimestamp() { return this.wagersTime.longValueExact(); }

}
