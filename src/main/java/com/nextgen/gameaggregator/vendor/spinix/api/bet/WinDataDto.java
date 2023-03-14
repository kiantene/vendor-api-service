package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WinDataDto implements WinData {
    private String externalTransactionId;
    private BigDecimal amount;
    private String roundId;
    private String gameId;
    private Long timestamp;
    private WinType winType;
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
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameId;
    }

    @Override
    public Long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public WinType getWinType() {
        return this.winType;
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

