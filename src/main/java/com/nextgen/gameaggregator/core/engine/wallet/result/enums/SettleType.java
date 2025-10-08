package com.nextgen.gameaggregator.core.engine.wallet.result.enums;

public enum SettleType {
    /**
     * Settled by transaction
     * - Every bet or result transaction will produce 1 bet history record
     */
    TRANSACTION,

    /**
     * Settled by individual bet
     * - Every bet transaction will be settled by a result transaction and produce 1 bet history record
     */
    BET,

    /**
     * Settled by round
     * - All transactions within the same round will be grouped and produce 1 bet history record
     */
    ROUND
}
