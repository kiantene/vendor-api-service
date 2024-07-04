package com.nextgen.gameaggregator.operator.sport.refund;

import com.nextgen.gameaggregator.core.WalletRequest;
import lombok.Data;

@Data
public class SportRefundDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private String currency;
    private String gameCode;
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
        this.currency = walletRequest.getCurrencyCode();
        this.gameCode = walletRequest.getGameCode();
        this.timestamp = walletRequest.getTimestamp();
    }
}
