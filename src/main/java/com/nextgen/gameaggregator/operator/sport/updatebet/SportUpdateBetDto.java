package com.nextgen.gameaggregator.operator.sport.updatebet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.core.WalletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SportUpdateBetDto {
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

    @NotNull(message = "newBetAmount cannot be null")
    private BigDecimal newBetAmount;

    @NotNull(message = "creditAmount cannot be null")
    private BigDecimal creditAmount;

    @NotBlank(message = "gameCode cannot be blank")
    private String gameCode;

    @NotBlank(message = "currency cannot be blank")
    private String currency;

    @NotNull(message = "timestamp cannot be null")
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
