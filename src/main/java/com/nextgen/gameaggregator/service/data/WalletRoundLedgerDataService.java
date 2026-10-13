package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Persistence facade for {@link WalletRoundLedger} documents in the
 * {@code wallet_round_ledgers} collection.
 *
 * <p>Doc id: {@code <vendorClassName>::<vendorPlayerUsername>::<vendorRoundId>}.
 *
 * <p>The ledger is created on the first debit of a round via {@link #insertOrGet}.
 * Each subsequent successful txn appends a slim {@link WalletRoundTxnRef} via
 * {@link #appendTxnRef} (CAS-aware {@code arrayAppend} + {@code increment("txnCount", 1)}
 * + {@code upsert("vendorLastBalance", ...)}, all atomic in one {@code mutateIn}).
 * {@link #markSettled} fires once when the framework observes
 * {@code !isRefund && isRoundEnded} on a credit success.
 */
public interface WalletRoundLedgerDataService {

    /**
     * Returns {@code Optional.empty()} on a fresh insert, or {@code Optional.of(existing)}
     * when the ledger already exists for this round. Mirrors
     * {@link GameRoundDataService#insertOrGet}.
     */
    Optional<KvDoc<WalletRoundLedger>> insertOrGet(WalletRoundLedger ledger);

    /** KV lookup; returns {@code null} when not found. CAS-aware via {@link KvDoc#getCas()}. */
    KvDoc<WalletRoundLedger> findById(String id);

    /**
     * CAS-aware atomic mutation: append {@code ref} to {@code transactions[]},
     * increment {@code txnCount} by 1, and upsert {@code vendorLastBalance}.
     *
     * @param cas CAS captured from a previous {@link #findById}; mismatch triggers retry inside the impl
     */
    void appendTxnRef(String ledgerId, WalletRoundTxnRef ref, BigDecimal vendorLastBalance, long cas);

    /**
     * CAS-update {@code state = SETTLED} and {@code isEnded = true}. Called once per round
     * when a credit succeeds and the vendor mapper signaled {@code isRoundEnded = true}.
     */
    void markSettled(String ledgerId);
}
