package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;

import java.math.BigDecimal;

public class SettledDto extends ActionsWagerInfoDto implements SportBetResultData {
    @Override
    public String getExternalTransactionId() {
        return this.getWagerId().toString();
    }

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
    public BigDecimal getBetAmount() {
        return this.getStake();
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return this.getStake();
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getToWin();
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.getStake();
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }

    @Override
    public Integer getBetType() {
        return this.getType().equalsIgnoreCase("PARLAY") ? BetType.PARLAY_BET.code : BetType.NORMAL_BET.code;
    }
}
