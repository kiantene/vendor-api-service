package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.service.CustomBooleanDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

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
    @Range(min = 0, max = 2147483647)
    private BigInteger wagersTime;
    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 4)
    private BigDecimal betAmount;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal winloseAmount;

    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    private Boolean isFreeRound;

    // Transaction ID used when Free Spin
    @Positive
    private BigInteger transactionId;

    // Optional fields, not used for any processing
    private String userId;
    private String platform;
    private Integer statementType;
    private Integer gameCategory;
    // End Optional fields

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.round);
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.round);
    }

    @Override
    public String getRoundId() {
        return (Optional.ofNullable(this.isFreeRound).orElse(Boolean.FALSE) && (this.transactionId != null)) ? String.valueOf(this.transactionId) : String.valueOf(this.round);
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.game);
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.winloseAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return getWinloseAmount();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.betAmount;
    }

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
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        Boolean isFreeSpin = Optional.ofNullable(this.isFreeRound).orElse(Boolean.FALSE);
        return isFreeSpin ? 1 : 0;

    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }


    private Long getTimestamp() {
        long timestamp = this.getWagersTime().longValueExact();
        if (String.valueOf(Math.abs(timestamp)).length() > 10) {
            return timestamp;
        }
        return timestamp * 1000;
    }
}
