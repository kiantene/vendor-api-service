package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim discovery ref appended to {@link WalletRoundLedger#getTransactions()}.
 * Five fields keyed by {@link #docId} for direct KV lookup of the live txn doc.
 *
 * <p><strong>Snapshot at append, not synced.</strong> {@link #state} and {@link #status}
 * capture the values at the moment this ref was appended. The authoritative live state
 * always lives on the standalone {@link DebitTxn}/{@link CreditTxn} doc at {@link #docId};
 * callers do {@code collection.get(ref.getDocId())} to read it. We don't cascade-update
 * the ref when a CreditProcessor CAS-flips a DebitTxn's state — that would double the
 * CAS contention surface (debit doc + ledger array on every state change) and require
 * locating-and-mutating-by-id inside the array.
 *
 * <p>{@link #kind} is a literal {@code "DEBIT"} / {@code "CREDIT"} string — matches the
 * {@code @JsonTypeInfo} discriminator on {@link WalletTxn}. Letting callers filter
 * the ledger array without parsing the {@link #docId}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletRoundTxnRef {

    /** Primary lookup target — full Couchbase doc id of the txn this ref points to. */
    @JsonProperty("docId")
    private String docId;

    /** Discriminator string: {@code "DEBIT"} or {@code "CREDIT"}. Matches {@link WalletTxn}'s {@code kind} JSON property. */
    @JsonProperty("kind")
    private String kind;

    /** Platform-side UUIDv7 for this txn — cross-link key, same value as {@code WalletTxn.gaTxnId}. */
    @JsonProperty("gaTxnId")
    private String gaTxnId;

    /** Snapshot of the txn's reference-state at append time. NOT synced with the txn doc. */
    @JsonProperty("state")
    private GameRoundState state;

    /** Snapshot of the txn's transport status at append time. NOT synced with the txn doc. */
    @JsonProperty("status")
    private TxnStatus status;

    /** Snapshot ref of a debit txn. */
    public static WalletRoundTxnRef of(DebitTxn txn) {
        return new WalletRoundTxnRef(
                txn.getId(),
                "DEBIT",
                txn.getGaTxnId(),
                txn.getState(),
                txn.getStatus()
        );
    }

    /** Snapshot ref of a credit txn (settle or refund — discriminator is the credit's {@code state}). */
    public static WalletRoundTxnRef of(CreditTxn txn) {
        return new WalletRoundTxnRef(
                txn.getId(),
                "CREDIT",
                txn.getGaTxnId(),
                txn.getState(),
                txn.getStatus()
        );
    }
}
