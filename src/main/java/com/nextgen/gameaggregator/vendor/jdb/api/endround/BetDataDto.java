package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDataDto implements BetData {
    private String externalTransactionId;
    private BigDecimal amount;
    private String roundId;
    private String gameId;
    private Long timestamp;

    @Override
    public BigDecimal getAmount(){
        return this.amount.abs();
    }
}
