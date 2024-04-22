package com.nextgen.gameaggregator.vendor.bgaming.api.endround;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EndRoundDto extends CommonDto implements BetResultData {
    @JsonIgnore
    private Boolean isSettled;

    @Override
    public String getExternalTransactionId() {
        if (this.isSettled) {
            return this.getVendorRoundId();
        }
        return this.getActionDto().getActionId();
    }

    @Override
    public String getVendorBetId() {
        if (this.getActionDto() != null) {
            return this.getActionDto().getActionId();
        }
        return this.getVendorRoundId();
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
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.getActionDto() != null && this.getActionDto().getAction().equals("win")) {
            return new BigDecimal(this.getActionDto().getAmount());
        }
        return null;
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
        return VendorService.getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return VendorService.getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getTimestamp();
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
        if (this.getFinished() && this.getIsSettled()) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
