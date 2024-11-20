package com.nextgen.gameaggregator.entity.warehouse;

import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDetailUrlInfo implements IBetDetailUrlInfo {

    private String betId;
    private String transactionId;
    private String externalTransactionId;
    private String externalRoundId;
    private String vendorBetId;
    private String username;
    private Integer currencyId;
    private String currencyCode;
    private String vendorCurrencyCode;
    private String gameCode;
    private Integer vendorId;
    private String vendorCode;
    private String gameCategoryCode;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private BigDecimal jackpotAmount;
    private BigDecimal refundAmount;
    private Integer status;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Integer vendorLineId;
    private String isFreeSpin;
    private String vendorUsername;
    private String gameSessionToken;

    // Constructors
    public BetDetailUrlInfo() {
    }

    // Full constructor for all fields
    public BetDetailUrlInfo(String betId, String transactionId, String externalTransactionId,
                            String externalRoundId, String vendorBetId, String username, Integer currencyId,
                            String currencyCode, String vendorCurrencyCode, String gameCode,
                            Integer vendorId, String vendorCode, String gameCategoryCode,
                            BigDecimal betAmount, BigDecimal winAmount, BigDecimal winLoss,
                            BigDecimal effectiveTurnover, BigDecimal jackpotAmount,
                            BigDecimal refundAmount, Integer status, Long vendorBetTime,
                            Long vendorSettleTime, Integer vendorLineId, String isFreeSpin,
                            String vendorUsername, String gameSessionToken) {
        this.betId = betId;
        this.transactionId = transactionId;
        this.externalTransactionId = externalTransactionId;
        this.externalRoundId = externalRoundId;
        this.vendorBetId = vendorBetId;
        this.username = username;
        this.currencyId = currencyId;
        this.currencyCode = currencyCode;
        this.vendorCurrencyCode = vendorCurrencyCode;
        this.gameCode = gameCode;
        this.vendorId = vendorId;
        this.vendorCode = vendorCode;
        this.gameCategoryCode = gameCategoryCode;
        this.betAmount = betAmount;
        this.winAmount = winAmount;
        this.winLoss = winLoss;
        this.effectiveTurnover = effectiveTurnover;
        this.jackpotAmount = jackpotAmount;
        this.refundAmount = refundAmount;
        this.status = status;
        this.vendorBetTime = vendorBetTime;
        this.vendorSettleTime = vendorSettleTime;
        this.vendorLineId = vendorLineId;
        this.isFreeSpin = isFreeSpin;
        this.vendorUsername = vendorUsername;
        this.gameSessionToken = gameSessionToken;
    }
}

