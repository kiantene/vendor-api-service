package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditSlotTransactionsDto extends CreditTransactionsDto {

    @Override
    public String getExternalTransactionId() {
        return this.getPtxid();
    }

    @Override
    public String getVendorBetId() {
        return this.getPtxid();
    }

    @Override
    public String getRoundId() {
        return this.getExternalroundid();
    }

    @Override
    public String getGameId() {
        return this.getGamecode();
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getAmt();
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.getAmt();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
            return VendorService.convertToTimestamp(this.getTimestamp());
    }

    @Override
    public Long getResultTime() {
        return VendorService.convertToTimestamp(this.getTimestamp());
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.convertToTimestamp(this.getTimestamp());
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
        if (this.getIsclosinground() != null && this.getIsclosinground()) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
