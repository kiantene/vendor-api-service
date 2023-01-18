package com.nextgen.gameaggregator.controller.pgsoftbettransactionlist;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetHistoryResponseVo {
    private String betId;
    private String parentBetId;
    private String playerName;
    private String currency;
    private Integer gameId;
    private Integer platform;
    private Integer betType;
    private Integer transactionType;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal jackpotRtpContributionAmount;
    private BigDecimal jackpotContributionAmount;
    private BigDecimal jackpotWinAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Integer handsStatus;
    private Long rowVersion;
    private Long betTime;
    private Long betEndTime;
    private Boolean isFeatureBuy;
}
