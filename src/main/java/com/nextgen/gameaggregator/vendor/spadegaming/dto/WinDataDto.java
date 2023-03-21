package com.nextgen.gameaggregator.vendor.spadegaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WinDataDto implements WinData {
    private String externalTransactionId;
    private BigDecimal amount;
    private String roundId;
    private String gameId;
    private Long timestamp;
    private WinType winType;
    private BigDecimal effectiveTurnover;
    @Override
    public BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog) {
        return betResultLog;
    }
}
