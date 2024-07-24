package com.nextgen.gameaggregator.operator.wallet.rollback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletRollbackDto {
    @NotBlank(message = "traceId cannot be blank")
    private String traceId;

    @NotBlank(message = "transactionId cannot be blank")
    private String transactionId;

    @NotBlank(message = "betId cannot be blank")
    private String betId;

    @NotBlank(message = "externalTransactionId cannot be blank")
    private String externalTransactionId;

    @NotBlank(message = "roundId cannot be blank")
    private String roundId;

    @NotBlank(message = "gameCode cannot be blank")
    private String gameCode;

    @NotBlank(message = "username cannot be blank")
    private String username;

    @NotBlank(message = "currency cannot be blank")
    private String currency;

    @NotNull(message = "timestamp cannot be blank")
    private Long timestamp;
}
