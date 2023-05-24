package com.nextgen.gameaggregator.vendor.ezugi.api.debit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto extends CommonDto implements BetResultData {

    private String uid;
    private String transactionId;
    @JsonProperty("roundId")
    private String vendorRoundId;
    private String gameId;
    private String tableId;
    private Double debitAmount;
    private Integer betTypeID;

    @Override
    public String getExternalTransactionId() {
        return this.transactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.transactionId;
    }

    @Override
    @JsonIgnore
    public String getRoundId() {
        return this.transactionId;
    }

    @Override
    public String getGameId() {
        return this.gameId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.valueOf(this.debitAmount);
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
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
        return getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        if(betTypeID.equals(BetTypeID.TIP)){
            return getTimestamp();
        }
        return null;
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
        if(betTypeID.equals(BetTypeID.TIP)){
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
