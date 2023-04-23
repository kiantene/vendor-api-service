package com.nextgen.gameaggregator.vendor.jili.api.kiv_sessionbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.Data;

import java.math.BigDecimal;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionWinDto implements WinData {
    private String externalTransactionId;
    private BigDecimal amount;
    private String roundId;
    private String gameId;
    private Long timestamp;
    private ResultType resultType;
    private BigDecimal effectiveTurnover;

    @Override
    public ResultType getWinType() {
        return null;
    }

    @Override
    public BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog) {
        return betResultLog;
    }
}
