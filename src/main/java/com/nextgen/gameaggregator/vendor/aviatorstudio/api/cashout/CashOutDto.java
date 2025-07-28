package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.aviatorstudio.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashOutDto extends CommonDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return this.getTransactionId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransactionId();
    }

    @Override
    public String getRoundId() {
        return this.getVendorRoundId();
    }

    @Override
    public String getGameId() {
        return this.getVendorGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getAmount();
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
        return this.getAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
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
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
