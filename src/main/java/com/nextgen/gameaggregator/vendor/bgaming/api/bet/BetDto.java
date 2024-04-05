package com.nextgen.gameaggregator.vendor.bgaming.api.bet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDto extends CommonDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return this.getVendorRoundId();
    }

    @Override
    public String getVendorBetId() {
        return this.getActionDto().getActionId();
    }

    @Override
    public String getRoundId() {
        return this.getVendorRoundId();
    }

    @Override
    public String getGameId() {
        return this.getGame();
    }

    @Override
    public BigDecimal getBetAmount() {
        return new BigDecimal(this.getActionDto().getAmount());
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
        return new BigDecimal(this.getActionDto().getAmount());
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimestamp();
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
