package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BetDto implements BetData {
    private String externalTransactionId;
    private String roundId;
    private BigDecimal amount;
    private String gameId;
    private Long timestamp;
}
