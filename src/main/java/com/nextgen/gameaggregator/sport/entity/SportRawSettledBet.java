package com.nextgen.gameaggregator.sport.entity;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class SportRawSettledBet implements SportBetResultData {
    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private String gameId;
    private String vendorPlayerUsername;
    private BigDecimal betAmount;
    private BigDecimal newBetAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Long vendorBetTime;
    private Long resultTime;
    private Long vendorSettleTime;
    private BetStatus betStatus;
    private Integer betType;

}
