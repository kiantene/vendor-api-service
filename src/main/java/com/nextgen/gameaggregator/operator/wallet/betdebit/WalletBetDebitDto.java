package com.nextgen.gameaggregator.operator.wallet.betdebit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class WalletBetDebitDto {

    @NotBlank(message = "traceId cannot be blank")
    private String traceId;
    @NotBlank(message = "username cannot be blank")
    private String username;
    @NotBlank(message = "transactionId cannot be blank")
    private String transactionId;
    //    @NotBlank(message = "externalTransactionId cannot be blank")
//    private String externalTransactionId;
    @NotBlank(message = "roundId cannot be blank")
    private String roundId;
    //    @NotNull(message = "takeAll cannot be null")
//    private Integer takeAll;
    @NotNull(message = "amount cannot be null")
    private BigDecimal amount;
    @NotBlank(message = "currency cannot be blank")
    private String currency;
    @NotBlank(message = "gameCode cannot be blank")
    private String gameCode;
    @NotBlank(message = "token cannot be blank")
    private String token;
    @NotNull(message = "timestamp cannot be null")
    private Long timestamp;
}
