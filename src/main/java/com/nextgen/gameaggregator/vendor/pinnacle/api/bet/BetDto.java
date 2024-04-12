package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;

import java.math.BigDecimal;

public class BetDto extends ActionsWagerInfoDto implements SportBetResultData {
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
        return null;
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
        return VendorService.convertDateTimeStringToTimestamp(this.getTransactionDate(), Formats.DATE_TIME_FORMAT_T_SEPARATOR, Formats.GMT_MINUS_FOUR);
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
