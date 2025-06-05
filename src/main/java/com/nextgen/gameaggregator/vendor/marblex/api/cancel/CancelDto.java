package com.nextgen.gameaggregator.vendor.marblex.api.cancel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelDto extends CommonDto implements SportUnsettleData {


    @JsonProperty("RoundID")
    private String roundId;


    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;


    @JsonProperty("PlatformTransactionID")
    private String platformTransactionID;


    @JsonProperty("TransactionTime")
    private String transactionTime;

    @Override
    public String getExternalTransactionId() {
        return this.platformTransactionID;
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
