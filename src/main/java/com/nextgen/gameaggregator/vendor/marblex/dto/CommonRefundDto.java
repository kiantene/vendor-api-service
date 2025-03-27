package com.nextgen.gameaggregator.vendor.marblex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommonRefundDto extends CommonDto implements SportRefundData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("RoundID")
    private String roundId;

    @NotBlank
    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;

    @NotBlank
    @JsonProperty("TransactionTime")
    private String transactionTime;

    @Override
    public String getExternalTransactionId() {
        return this.janusTransactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.janusTransactionId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.getPlayerId();
    }

    @Override
    public Long getTimestamp() {
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
    }
}
