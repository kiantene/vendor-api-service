package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WinDto implements WinData {
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
