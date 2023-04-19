package com.nextgen.gameaggregator.operator.wallet.betResult;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBetResultDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String externalRoundId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal winLoss;
    private BigDecimal jackpotAmount;
    private String resultType;
    private Integer isFreespin;
    private Integer isEndRound;
    private String currency;
    private String token;
    private String gameCode;
    private Long betTime;
    private Long settledTime;
}
