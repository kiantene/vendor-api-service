package com.nextgen.gameaggregator.vendor.habanero.api.pokerresult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.constant.GameStateMode;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokerResultDto extends FundInfoDto implements BetResultData {

    private String roundId;

    private String gameId;

    @Override
    public String getExternalTransactionId() {
        return this.getTransferId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransferId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getAmount().abs();
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.getWinAmount().subtract(this.getBetAmount());
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.getBetAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.dateTimeConvert(this.getDtEvent());
    }

    @Override
    public Long getResultTime() {
        return VendorService.dateTimeConvert(this.getDtEvent());
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.dateTimeConvert(this.getDtEvent());
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
        return BetStatus.SETTLED;
    }
}
