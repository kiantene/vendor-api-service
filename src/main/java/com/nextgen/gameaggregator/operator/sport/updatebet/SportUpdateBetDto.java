package com.nextgen.gameaggregator.operator.sport.updatebet;

import com.nextgen.gameaggregator.core.WalletRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class SportUpdateBetDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal newBetAmount;
    private BigDecimal creditAmount;
    private String gameCode;
    private String currency;
    private Long timestamp;

    public SportUpdateBetDto(WalletRequest walletRequest, BigDecimal conversionRate) {
        this.traceId = walletRequest.getTraceId();
        this.username = walletRequest.getOperatorUsername();
        this.transactionId = walletRequest.getTransactionId();
        this.externalTransactionId = walletRequest.getExternalTransactionId();
        this.betId = walletRequest.getBetId();
        this.roundId = Objects.requireNonNullElse(walletRequest.getNewRoundId(), walletRequest.getRoundId());
        this.gameCode = walletRequest.getGameCode();
        this.currency = walletRequest.getCurrencyCode();
        this.timestamp = walletRequest.getVendorBetTime();

        BigDecimal betAmt = walletRequest.getBetAmount();
        BigDecimal newBetAmt = walletRequest.getNewBetAmount();
        BigDecimal creditAmt = betAmt.subtract(newBetAmt);

        this.betAmount = new BigDecimal(betAmt.multiply(conversionRate).stripTrailingZeros().toPlainString());
        this.newBetAmount = new BigDecimal(newBetAmt.multiply(conversionRate).stripTrailingZeros().toPlainString());
        this.creditAmount = new BigDecimal(creditAmt.multiply(conversionRate).stripTrailingZeros().toPlainString());
    }
}
