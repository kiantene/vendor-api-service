package com.nextgen.gameaggregator.operator.wallet.settled;

import com.nextgen.gameaggregator.enums.WinType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletSettledDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String referenceTransactionId;
    private BigDecimal amount;
    private String currency;
    private String token;
    private String gameCode;
    private String roundId;
    private WinType winType;
    private Long timestamp;
}
