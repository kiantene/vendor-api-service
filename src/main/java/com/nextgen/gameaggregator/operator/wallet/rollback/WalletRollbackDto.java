package com.nextgen.gameaggregator.operator.wallet.rollback;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    /**
     * Operator-POV reversal amounts ({@code betAmount} / {@code winAmount}, jackpot folded into winAmount).
     * Populated ONLY for transfer-wallet (seamless transfer) agents; null for normal operators,
     * who decide their own reversal. {@code @JsonInclude(NON_NULL)} keeps the field out of the
     * payload entirely for normal operators (not serialized as {@code "meta": null}).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RollbackMeta meta;
}
