package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WinDataDto implements WinData {
    private String externalTransactionId;
    private BigDecimal amount;
    private String roundid;
    private String gamecode;
    private Long timestamp;
    private ResultType resultType;
    private BigDecimal effectiveTurnover;

    @Override
    public String getExternalTransactionId() {
        return this.externalTransactionId;
    }

    @Override
    public BigDecimal getAmount() {
        return this.amount;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
    }

    @Override
    public String getGameId() {
        return this.gamecode;
    }

    @Override
    public Long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public ResultType getWinType() {
        return this.resultType;
    }

    public ResultType getResultType() {
        return this.resultType;
    }

    @Override
    public BigDecimal getEffectiveTurnover(){
        return this.effectiveTurnover;
    }

    @Override
    public BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog) {
        return betResultLog;
    }
}
