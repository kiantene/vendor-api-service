package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class BetDto implements BetData {
    private String action;

    @NotBlank
    @Size(min = 12, max = 12)
    private Long ts;
    private Long transferId;
    private String uid;
    private String currency;
    private BigDecimal amount;
    private Long gameRoundSeqNo;
    @JsonProperty("mType")
    private Integer mType;

    @Override
    public String getExternalTransactionId() {
        return this.transferId.toString();
    }

    @Override
    public String getRoundId() {
        return this.gameRoundSeqNo.toString();
    }

    @Override
    public String getGameId() {
        return this.mType.toString();
    }

    @Override
    public Long getTimestamp() {
        return this.ts;
    }
}
