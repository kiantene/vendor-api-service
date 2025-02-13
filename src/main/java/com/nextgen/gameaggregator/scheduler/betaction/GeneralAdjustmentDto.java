package com.nextgen.gameaggregator.scheduler.betaction;

import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class GeneralAdjustmentDto implements AdjustmentData {

    private String vendorBetId;
    private String roundId;
    private String externalTransactionId;
    private String gameId;
    private BigDecimal adjustmentAmount;
    private Long timestamp;

    @Override
    public String getVendorBetId() {
        return this.vendorBetId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getExternalTransactionId() {
        return this.externalTransactionId;
    }

    @Override
    public String getGameId() {
        return this.gameId;
    }

    @Override
    public BigDecimal getAdjustmentAmount() {
        return this.adjustmentAmount;
    }

    @Override
    public Long getTimestamp() {
        return this.timestamp;
    }
}
