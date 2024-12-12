package com.nextgen.gameaggregator.operator.wallet.betcredit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class WalletBetCreditDto {

    @NotBlank(message = "traceId cannot be blank")
    private String traceId;
    @NotBlank(message = "username cannot be blank")
    private String username;
    @NotBlank(message = "transactionId cannot be blank")
    private String transactionId;
    @NotBlank(message = "betId cannot be blank")
    private String betId;
    @NotBlank(message = "roundId cannot be blank")
    private String roundId;
    @NotNull(message = "isRefund cannot be null")
    private Integer isRefund = 0;
    @NotNull(message = "amount cannot be null")
    private BigDecimal amount;
    @NotNull(message = "bet amount cannot be null")
    private BigDecimal betAmount;
    @NotNull(message = "win amount cannot be null")
    private BigDecimal winAmount;
    @NotNull(message = "effective turnover cannot be null")
    private BigDecimal effectiveTurnover;
    @NotNull(message = "win loss cannot be null")
    private BigDecimal winLoss;
    @NotNull(message = "jackpot amount cannot be null")
    private BigDecimal jackpotAmount;
    @NotBlank(message = "currency cannot be blank")
    private String currency;
    @NotBlank(message = "token cannot be blank")
    private String token;
    @NotBlank(message = "gameCode cannot be blank")
    private String gameCode;
    @NotNull(message = "bet time cannot be null")
    private Long betTime;
    @NotNull(message = "settled time cannot be null")
    private Long settledTime;
    @NotNull(message = "timestamp cannot be null")
    private Long timestamp;
}
