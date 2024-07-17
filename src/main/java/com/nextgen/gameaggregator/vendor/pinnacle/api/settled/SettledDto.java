package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Setter
@Getter
public class SettledDto extends ActionsWagerInfoDto implements SportBetResultData {
    private BigDecimal transactionAmount;
    private String externalTransactionId;

    @Override
    public String getVendorBetId() {
        return this.getWagerId().toString();
    }

    @Override
    public String getRoundId() {
        if (isMultipleBet()) {
            return this.getWagerMasterId().toString();
        } else {
            return this.getWagerId().toString();
        }
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
        return this.getTransactionAmount();
    }

    @Override
    public BigDecimal getWinLoss() {
        return Optional.ofNullable(this.getProfitAndLoss()).orElse(this.getWinAmount().subtract(this.getBetAmount()));
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
        return this.getVendorSettleTime();
    }

    @Override
    public Long getVendorSettleTime() {
        String dateTimeString = Objects.requireNonNullElse(this.getResettlementTime(), this.getSettlementTime());
        return VendorService.convertDateTimeStringToTimestamp(dateTimeString, Formats.DATE_TIME_FORMAT);
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }

    @Override
    public Integer getBetType() {
        return this.getType().equalsIgnoreCase("PARLAY") ? BetType.PARLAY_BET.code : BetType.NORMAL_BET.code;
    }

    private boolean isMultipleBet() {
        boolean isMultipleBet = false;
        if (Objects.nonNull(this.getWagerMasterId())) {
            isMultipleBet = !this.getWagerId().toString().equalsIgnoreCase(this.getWagerMasterId().toString());
        }
        return isMultipleBet;
    }
}
