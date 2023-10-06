package com.nextgen.gameaggregator.vendor.habanero.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends FundInfoDto implements BetResultData {

    private String vendorBetId;

    private String roundId;

    private String gameId;

    @Override
    public String getExternalTransactionId() {
        return this.getTransferId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getAmount().abs();
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
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.dateTimeConvert(this.getDtEvent());
    }

    @Override
    public Long getResultTime() { return null; }

    @Override
    public Long getVendorSettleTime() { return null; }

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
