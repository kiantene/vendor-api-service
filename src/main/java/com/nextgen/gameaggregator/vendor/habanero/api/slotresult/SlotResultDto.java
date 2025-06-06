package com.nextgen.gameaggregator.vendor.habanero.api.slotresult;

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
public class SlotResultDto extends FundInfoDto implements BetResultData {

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
        BigDecimal winAmount = this.getAmount().abs();
        if (this.getJpWin()) {
            //jackpot
            winAmount = BigDecimal.ZERO;
        }
        return winAmount;
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
        BigDecimal jackpotAmount = null;
        if (this.getJpWin()) {
            //jackpot
            jackpotAmount = this.getAmount().abs();
        }
        return jackpotAmount;
    }

    @Override
    public Integer getIsFreespin() {
        Integer isFreespin = 0;
        if (this.getIsBonus()) {
            //bonus free spin
            isFreespin = 1;
        } else if (this.getGameStateMode().equals(GameStateMode.COUTINUEATION) && !this.getJpWin()) {
            //free spin
            isFreespin = 1;
        }
        return isFreespin;
    }

    @Override
    public BetStatus getBetStatus() {
        if (this.getGameStateMode().equals(GameStateMode.ENDROUND) || this.getGameStateMode().equals(GameStateMode.EXPIRE)) {
            //handle settle bet
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
