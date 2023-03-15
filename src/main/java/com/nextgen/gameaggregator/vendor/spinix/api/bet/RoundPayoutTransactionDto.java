package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoundPayoutTransactionDto {

    public BigDecimal amount;
    public String timestamp;
    public String reqId;
    public String info;
    public Boolean isEnd;
    public String type;

    public Long getTimestamp() {
        Instant instant = Instant.parse(this.timestamp);
        return instant.getEpochSecond();
    }
}


