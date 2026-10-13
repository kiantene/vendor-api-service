package com.nextgen.gameaggregator.core.engine.operator.wallet.debit;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Outbound DTO for the modern operator-call debit path.
 * <p>
 * Wire shape mirrors the legacy {@code com.nextgen.gameaggregator.operator.wallet.betdebit.WalletBetDebitDto}
 * so the operator receives identical JSON regardless of which code path constructs it.
 * <p>
 * Dispatched by {@link DebitOperatorWalletAdapter} via {@code OperatorApiService.execute(...)} against
 * {@code EndPoints.WALLET_BET_DEBIT}.
 */
@Data
public class OperatorDebitRequest implements OperatorRequestObject {
    private String traceId;
    private String username;
    private String transactionId;
    private String roundId;
    private BigDecimal amount;
    private String currency;
    private String gameCode;
    private String token;
    private Long timestamp;
}
