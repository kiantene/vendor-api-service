package com.nextgen.gameaggregator.vendor.habanero.api.result;

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
public class ResultDto extends FundInfoDto implements BetResultData {


    private String vendorBetId;

    private String roundId;

    private String gameId;

    @Override
    public String getExternalTransactionId() {
        return this.getTransferId();
    }

    @Override
    public BigDecimal getBetAmount() {
        BigDecimal betAmount = null;
        if(this.getIsBonus()){
            //bonus free spin
            betAmount = BigDecimal.ZERO;
        }
        return betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        BigDecimal winAmount = this.getAmount().abs();
        if(this.getJpWin()){
            //jackpot
            winAmount =  BigDecimal.ZERO;
        }
        return winAmount;
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
        Long vendorBetTime = null;
        if(this.getIsBonus()){
            //bonus free spin
            vendorBetTime = VendorService.dateTimeConvert(this.getDtEvent());
        }
        return vendorBetTime;
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
        if(this.getJpWin()){
            //jackpot
            jackpotAmount = this.getAmount().abs();
        }
        return jackpotAmount;
    }

    @Override
    public Integer getIsFreespin() {
        Integer isFreespin = 0;
        if(this.getIsBonus()){
            //bonus free spin
            isFreespin = 1;
        } else if(this.getGameStateMode().equals(GameStateMode.CREDIT) && !this.getJpWin()){
            //free spin
            isFreespin = 1;
        }
        return isFreespin;
    }

    @Override
    public BetStatus getBetStatus() {
        BetStatus betStatus = BetStatus.UNSETTLED;
        if (this.getGameStateMode().equals(GameStateMode.CREDIT_ENDROUND) || this.getGameStateMode().equals(GameStateMode.EXPIRE)) {
            //handle settle bet and bonus free spin
            betStatus = BetStatus.SETTLED;
        }
        return betStatus;
    }
}
