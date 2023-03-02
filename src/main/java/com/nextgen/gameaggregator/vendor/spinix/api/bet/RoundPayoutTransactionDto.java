package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoundPayoutTransactionDto {
    public String id;
    public String type;
    public BigDecimal amount;
    public String info;
    public Boolean isEnd;
    public String timestamp;

    public Long getRoundPayoutTransactionTimestamp() {
        Instant instant = Instant.parse(this.getTimestamp());
        return instant.getEpochSecond();
    }
}


