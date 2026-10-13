package com.nextgen.gameaggregator.enums;

public enum BetTransactionType {
    /** Legacy BET/RESULT/ROLLBACK family — emitted by {@code BetTransactionHistoryProducer}. */
    BET, RESULT, ROLLBACK_BET, ROLLBACK_RESULT,

    /**
     * Debit/credit framework — emitted by {@code WalletTransactionHistoryProducer}.
     * One per wallet movement; {@code DEBIT} covers Action 9 / equivalent,
     * {@code CREDIT} covers both settle (Action 10) and refund (Action 11) since both
     * are CREDIT-direction wallet movements. Refund vs settle is encoded on the
     * underlying {@code CreditTxn.state} field, not on this discriminator.
     */
    DEBIT, CREDIT;
}
