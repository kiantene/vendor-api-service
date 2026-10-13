package com.nextgen.gameaggregator.core.engine.operator.wallet.credit;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Outbound DTO for the modern operator-call credit path. Handles BOTH settle and rollback
 * — distinguished by the {@code isRefund} flag (matching legacy
 * {@code com.nextgen.gameaggregator.operator.wallet.betcredit.WalletBetCreditDto}).
 * <p>
 * Wire shape mirrors {@code WalletBetCreditDto} field-for-field so the operator receives
 * identical JSON regardless of which code path constructs it.
 * <p>
 * Dispatched by {@link CreditOperatorWalletAdapter} via {@code OperatorApiService.execute(...)}
 * against {@code EndPoints.WALLET_BET_CREDIT}.
 */
@Data
public class OperatorCreditRequest implements OperatorRequestObject {
    private String traceId;
    private String username;
    private String transactionId;
    private String betId;
    private String roundId;
    /** 0 = settlement; 1 = refund/rollback. */
    private Integer isRefund;
    private BigDecimal amount;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal winLoss;
    private BigDecimal jackpotAmount;
    private String currency;
    private String token;
    private String gameCode;
    private Long betTime;
    private Long settledTime;
    private Long timestamp;
}
