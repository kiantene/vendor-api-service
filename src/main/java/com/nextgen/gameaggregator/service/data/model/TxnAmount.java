package com.nextgen.gameaggregator.service.data.model;

import java.math.BigDecimal;

public class TxnAmount {
    private final BigDecimal amount;

    // e.g., 0.001 to convert 1,000,000 → 1,000
    // Usually vendor sends IDR 1,000,000 and we need to send IDR(K) 1,000 to client
    protected final BigDecimal denominationRate;

    public TxnAmount(BigDecimal amount, BigDecimal denominationRate) {
        this.amount = amount;
        this.denominationRate = denominationRate;
    }

    public static TxnAmount of(BigDecimal amount, BigDecimal denominationRate) {
        return new TxnAmount(amount, denominationRate);
    }

    public BigDecimal amount() {
        return safeMultiply(amount, denominationRate);
    }

    protected BigDecimal safeMultiply(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || amount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = amount.multiply(rate).stripTrailingZeros();
        return new BigDecimal(result.toPlainString());
    }

    protected BigDecimal safeSubtract(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null) {
            amount1 = BigDecimal.ZERO;
        }
        if (amount2 == null) {
            amount2 = BigDecimal.ZERO;
        }
        return amount1.subtract(amount2);
    }
}
