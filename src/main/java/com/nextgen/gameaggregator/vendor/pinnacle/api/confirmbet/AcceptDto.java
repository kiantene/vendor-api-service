package com.nextgen.gameaggregator.vendor.pinnacle.api.confirmbet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Optional;

@Getter
@Setter
public class AcceptDto extends ActionsWagerInfoDto implements SportBetResultData {
    private BigDecimal vendorNewBetAmount;
    private BigDecimal betAmount;
    private String externalTransactionId;

    @Override
    public String getVendorBetId() {
        return this.getWagerId().toString();
    }

    @Override
    public String getRoundId() {
        return this.getWagerId().toString();
    }

    @Override
    public String getGameId() {
        return this.getSportId().toString();
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return Optional.ofNullable(this.getVendorNewBetAmount()).orElse(this.getBetAmount());
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return Optional.ofNullable(this.getVendorNewBetAmount()).orElse(this.getBetAmount());
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }

    @Override
    public Integer getBetType() {
        return this.getType().equalsIgnoreCase("PARLAY") ? BetType.PARLAY_BET.code : BetType.NORMAL_BET.code;
    }
}
