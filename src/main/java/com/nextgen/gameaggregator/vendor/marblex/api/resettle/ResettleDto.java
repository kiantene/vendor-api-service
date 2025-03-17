package com.nextgen.gameaggregator.vendor.marblex.api.resettle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.sport.adjustment.SportAdjustmentData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResettleDto extends CommonDto implements SportAdjustmentData {

    @JsonProperty("RoundID")
    private String roundId;

    @JsonProperty("GameCode")
    private String gameCode;

    @JsonProperty("Winloss")
    private BigDecimal winLoss;

    @JsonProperty("ReturnToWallet")
    private BigDecimal returnToWallet;

    @JsonProperty("AdjustmentAmount")
    private BigDecimal adjustmentAmount;

    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;

    @JsonProperty("TransactionTime")
    private String transactionTime;

    @Override
    public String getVendorGameCode() {
        return this.gameCode;
    }

    @Override
    public String getVendorUsername() {
        return this.getPlayerId();
    }

    @Override
    public String getVendorBetId() {
        return this.janusTransactionId;
    }

    @Override
    public String getExternalTransactionId() {
        return this.janusTransactionId;
    }

    @Override
    public BigDecimal getAmount() {
        return this.adjustmentAmount;
    }

    @Override
    public Long getTimestamp() {
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
    }
}
