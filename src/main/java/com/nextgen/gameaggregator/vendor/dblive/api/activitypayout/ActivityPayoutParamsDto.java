package com.nextgen.gameaggregator.vendor.dblive.api.activitypayout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.dblive.constant.TransferType;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityPayoutParamsDto extends ActivityPayoutCommonDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.getTransferNo());
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.getTransferNo());
    }

    @Override
    public String getRoundId() {
        return String.valueOf(this.getTransferNo());
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {

        return this.getPayoutType().equals(TransferType.DEDUCTION) ? this.getPayoutAmount().abs() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getPayoutType().equals(TransferType.DEDUCTION) ? BigDecimal.ZERO : this.getPayoutAmount().abs();
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.getPayoutType().equals(TransferType.DEDUCTION) ? BigDecimal.ZERO : this.getPayoutAmount().abs();

    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.getPayoutType().equals(TransferType.DEDUCTION) ? this.getPayoutAmount().abs() : BigDecimal.ZERO;
    }

    @Override
    public Long getVendorBetTime() {
        return this.getPayoutTime();
    }

    @Override
    public Long getResultTime() {
        return this.getPayoutTime();
    }

    @Override
    public Long getVendorSettleTime() {
        return this.getPayoutTime();
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
        return BetStatus.SETTLED;
    }

}
