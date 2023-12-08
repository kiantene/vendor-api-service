package com.nextgen.gameaggregator.vendor.advantplay.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.advantplay.dto.BetSettleRefundDto;
import com.nextgen.gameaggregator.vendor.advantplay.service.VendorService;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SettleDto extends BetSettleRefundDto implements BetResultData {
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal totalStake;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal totalWin;
    @NotBlank
    private String settleTime;
    @NotNull
    private Boolean hitJackpot;

    @Override
    public String getExternalTransactionId() {
        return this.getGameRoundId();
    }

    @Override
    public String getVendorBetId() {
        return this.getGameRoundId();
    }

    @Override
    public String getRoundId() {
        return this.getGameRoundId();
    }

    @Override
    public String getGameId() {
        return this.getGameCode();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getTotalStake();
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getTotalWin();
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.getTotalWin().subtract(this.getTotalStake());
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return VendorService.dateTimeConvert(this.getSettleTime());
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.dateTimeConvert(this.getSettleTime());
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {

        BetStatus betStatus = BetStatus.SETTLED;

//        if(this.getHitJackpot()){
//            betStatus = BetStatus.UNSETTLED;
//        }

        return betStatus;
    }


}
