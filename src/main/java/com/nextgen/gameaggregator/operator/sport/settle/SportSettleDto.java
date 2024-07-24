package com.nextgen.gameaggregator.operator.sport.settle;

import com.nextgen.gameaggregator.core.WalletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class SportSettleDto {
    @NotBlank(message = "traceId cannot be blank")
    private String traceId;

    @NotBlank(message = "username cannot be blank")
    private String username;

    @NotBlank(message = "transactionId cannot be blank")
    private String transactionId;

    @NotBlank(message = "externalTransactionId cannot be blank")
    private String externalTransactionId;

    @NotBlank(message = "betId cannot be blank")
    private String betId;

    @NotBlank(message = "roundId cannot be blank")
    private String roundId;

    @NotNull(message = "betAmount cannot be null")
    private BigDecimal betAmount;

    @NotNull(message = "winAmount cannot be null")
    private BigDecimal winAmount;

    @NotNull(message = "effectiveTurnover cannot be null")
    private BigDecimal effectiveTurnover;

    @NotNull(message = "winLoss cannot be null")
    private BigDecimal winLoss;

    @NotBlank(message = "gameCode cannot be blank")
    private String gameCode;

    @NotBlank(message = "currency cannot be blank")
    private String currency;
    
    @NotNull(message = "timestamp cannot be null")
    private Long timestamp;

    public SportSettleDto() {

    }

    public SportSettleDto(WalletRequest walletRequest, BigDecimal conversionRate) {
        this.traceId = walletRequest.getTraceId();
        this.username = walletRequest.getOperatorUsername();
        this.transactionId = walletRequest.getTransactionId();
        this.externalTransactionId = walletRequest.getExternalTransactionId();
        this.betId = walletRequest.getBetId();
        this.roundId = Objects.requireNonNullElse(walletRequest.getNewRoundId(), walletRequest.getRoundId());
        this.gameCode = walletRequest.getGameCode();
        this.currency = walletRequest.getCurrencyCode();
        this.timestamp = walletRequest.getVendorBetTime();

        BigDecimal betAmt = Objects.requireNonNullElse(walletRequest.getNewBetAmount(), walletRequest.getBetAmount());
        BigDecimal winAmt = walletRequest.getWinAmount();
        BigDecimal effectiveTurnoverAmt = walletRequest.getEffectiveTurnover();
        BigDecimal winLossAmt = walletRequest.getWinLoss();

        this.betAmount = new BigDecimal(betAmt.multiply(conversionRate).stripTrailingZeros().toPlainString());
        this.winAmount = new BigDecimal(winAmt.multiply(conversionRate).stripTrailingZeros().toPlainString());
        this.effectiveTurnover = new BigDecimal(effectiveTurnoverAmt.multiply(conversionRate).stripTrailingZeros().toPlainString());
        this.winLoss = new BigDecimal(winLossAmt.multiply(conversionRate).stripTrailingZeros().toPlainString());
    }
}
