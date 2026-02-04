package com.nextgen.gameaggregator.core.engine.operator.wallet.bet;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OperatorBetRequest implements OperatorRequestObject {
    private String traceId;
    private String username;
    private String transactionId;
    private String betId;
    private String externalTransactionId;
    private BigDecimal amount;
    private String currency;
    private String token;
    private String gameCode;
    private String roundId;
    private Long timestamp;
}
