package com.nextgen.gameaggregator.operator.wallet.betResult;

import com.nextgen.gameaggregator.enums.WinType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBetResultDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String externalRoundId;
    private String externalBetId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal winLoss;
    private BigDecimal jackpotAmount;
    private WinType winType;
    private Integer isFreespin;
    private Integer isEndRound;
    private Integer isCancelled;
    private String currency;
    private String token;
    private String gameCode;
    private Long betTime;
    private Long settledTime;
}
