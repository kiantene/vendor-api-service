package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {
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
    private boolean isFreeRound;
    private String userId;
    private BigInteger transactionId;
    private String platform;
    private Integer statementType;
    private Integer gameCategory;

    @Override
    public String getExternalTransactionId() { return String.valueOf(this.round); }
    @Override
    public String getVendorBetId(){ return String.valueOf(this.round); }
    @Override
    public String getRoundId() { return String.valueOf(this.round); }
    @Override
    public String getGameId() { return String.valueOf(this.game); }
    @Override
    public BigDecimal getBetAmount() { return this.betAmount; }
    @Override
    public BigDecimal getWinAmount() { return this.winloseAmount; }
    @Override
    public BigDecimal getWinLoss() { return getWinloseAmount().subtract(getBetAmount()); }

    @Override
    public BigDecimal getEffectiveTurnover() { return this.betAmount; }
    @Override
    public BigDecimal getRefundAmount() { return BigDecimal.ZERO;}
    @Override
    public Long getVendorBetTime() {
        return getTimestamp();
    }
    @Override
    public Long getResultTime() {
        return getTimestamp();
    }
    @Override
    public Long getVendorSettleTime() {
        return getTimestamp();
    }
    @Override
    public BigDecimal getJackpotAmount() { return BigDecimal.ZERO;}

    @Override
    public Integer getIsFreespin() { return 0;}

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }


    private Long getTimestamp() {
        long timestamp = this.getWagersTime().longValueExact();
        if(String.valueOf(Math.abs(timestamp)).length() > 10){
            return timestamp;
        }
        return timestamp * 1000;
    }
}
