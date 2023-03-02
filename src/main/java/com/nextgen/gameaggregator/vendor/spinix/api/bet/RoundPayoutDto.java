package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoundPayoutDto {
    public String userId;
    public String userToken;
    public String gameId;
    public String gameType;
    public String currency;
    public String roundId;
    public BigDecimal validTurnOver;
    public List<RoundPayoutTransactionDto> transactionList;
}
