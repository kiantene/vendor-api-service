package com.nextgen.gameaggregator.operator.wallet.betResult;


import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBetResultDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String betId;
    private String externalTransactionId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal winLoss;
    private BigDecimal jackpotAmount;
    private ResultType resultType;
    private Integer isFreespin;
    private Integer isEndRound;
    private String currency;
    private String token;
    private String gameCode;
    private Long betTime;
    private Long settledTime;
}
