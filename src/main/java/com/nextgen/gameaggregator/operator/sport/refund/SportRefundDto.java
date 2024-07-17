package com.nextgen.gameaggregator.operator.sport.refund;

import com.nextgen.gameaggregator.core.WalletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SportRefundDto {
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

    @NotBlank(message = "gameCode cannot be blank")
    private String gameCode;

    @NotBlank(message = "currency cannot be blank")
    private String currency;

    @NotNull(message = "timestamp cannot be null")
    private Long timestamp;

    public SportRefundDto() {
    }

    public SportRefundDto(WalletRequest walletRequest) {
        this.traceId = walletRequest.getTraceId();
        this.username = walletRequest.getOperatorUsername();
        this.transactionId = walletRequest.getTransactionId();
        this.externalTransactionId = walletRequest.getExternalTransactionId();
        this.betId = walletRequest.getBetId();
        this.roundId = walletRequest.getRoundId();
        this.gameCode = walletRequest.getGameCode();
        this.currency = walletRequest.getCurrencyCode();
        this.timestamp = walletRequest.getTimestamp();
    }
}
