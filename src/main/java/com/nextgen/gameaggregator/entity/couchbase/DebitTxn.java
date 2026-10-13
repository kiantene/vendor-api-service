package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Couchbase doc for a wallet debit operation issued by a vendor that speaks debit/credit
 * on the wire. Sibling of {@link CreditTxn} under {@link WalletTxn}.
 *
 * <p>The inherited {@code state} field carries the reference-state lifecycle:
 * <ul>
 *   <li>{@code UNSETTLED} (initial) — no successful {@link CreditTxn} has referenced this debit yet.</li>
 *   <li>{@code SETTLED} — a settle-mode {@link CreditTxn} has claimed this debit (set via Couchbase CAS).</li>
 *   <li>{@code REFUNDED} — a refund-mode {@link CreditTxn} has claimed this debit (set via Couchbase CAS).</li>
 * </ul>
 *
 * <p>Doc id: {@code <vendorClassName>::DEBIT::<vendorTxnId>}. Shared audit-header fields live on
 * {@link WalletTxn}. The two fields below are what a later credit naming this debit needs in place
 * of a game session (see {@code CreditSessionResolver}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DebitTxn extends WalletTxn {

    /**
     * Who was debited and how the operator was called: agent, agent player, operator currency, GA
     * game code and the operator session token, as resolved when the debit ran. Same shape and
     * purpose as {@code GameRound.agentMeta}: the credit goes back to where the money came from,
     * without looking the player's session or agent up again. {@code null} on debits written
     * before this field existed.
     */
    @JsonProperty("agentMeta")
    private AgentMeta agentMeta;

    /**
     * The vendor-side currency code the debit request carried (e.g. JDB {@code PP}). NOT the
     * operator currency ({@code agentCurrency}, {@code agentMeta.currency}). A credit's own vendor
     * currency is checked against this, as it would otherwise be checked against the session's
     * {@code vendorCurrencyCode}.
     */
    @JsonProperty("vendorCurrency")
    private String vendorCurrency;

    public DebitTxn() {
        super();
    }

    public static DebitTxn of(String vendorClassName, String vendorTxnId, long createdTimestamp) {
        DebitTxn txn = new DebitTxn();
        prepareAudit(txn, vendorClassName, vendorTxnId, createdTimestamp);
        return txn;
    }

    public static String createDocId(String vendorClassName, String vendorTxnId) {
        return vendorClassName + "::DEBIT::" + vendorTxnId;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return createDocId(getVendorClassName(), getVendorTxnId());
    }
}
