package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto implements BetResultData {
    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String account;

    @NotBlank
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

    @NotBlank
    private String data;

    @NotBlank
    private String createTime;

    @Positive
    @Digits(integer = 12, fraction = 10)
    private BigDecimal freegame;

    @Positive
    private BigDecimal bonus;

    @Positive
    private BigDecimal luckydraw;

    @Positive
    private BigDecimal jackpot;

    private List<@Positive @Digits(integer = 12, fraction = 10) BigDecimal> jackpotcontribution;

    private String betId;
    private String externalTransactionId;
    private BigDecimal winAmount;
    private Long resultTime;
    private Long vendorSettleTime;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Integer IsFreespin;

    @Override
    public String getExternalTransactionId() {
        return this.externalTransactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.roundid;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
    }

    @Override
    public String getGameId() {
        return this.gamecode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.winAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return this.resultTime;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.vendorSettleTime;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return this.jackpot;
    }

    @Override
    public Integer getIsFreespin() { return this.IsFreespin;}

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
