package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.gameaggregator.enums.GameRoundState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Couchbase doc for a wallet credit operation issued by a vendor that speaks debit/credit
 * on the wire. Sibling of {@link DebitTxn} under {@link WalletTxn}.
 *
 * <p>Handles both settlement and refund (rollback) via the inherited {@code state} field:
 * <ul>
 *   <li>{@code SETTLED} — this credit settles a prior debit.
 *       Populates the settle-mode analytics fields.</li>
 *   <li>{@code REFUNDED} — this credit refunds a prior debit.
 *       Populates the refund-linkage fields.</li>
 * </ul>
 *
 * <p>Doc id: {@code <vendorClassName>::CREDIT::<vendorTxnId>}. Shared audit-header on
 * {@link WalletTxn}; this leaf adds settle-mode analytics + refund-mode linkage.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreditTxn extends WalletTxn {

    /**
     * Original bet amount in vendor currency (pre-FX) — the wager being settled/refunded.
     * Analytics passthrough for operator reconciliation. Distinct from {@link WalletTxn#getVendorAmount()},
     * which is the wallet movement (payout for settle, refund value for refund).
     */
    @JsonProperty("vendorBetAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal vendorBetAmount;

    // --- Settle-mode (state == SETTLED) ---

    @JsonProperty("vendorWinAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal vendorWinAmount;

    /** Win/Loss from the player's perspective (may be negative). Operator's {@code winLoss}. */
    @JsonProperty("vendorWinLoss")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal vendorWinLoss;

    /** Effective turnover for rollover/rebate. Operator's {@code effectiveTurnover}. */
    @JsonProperty("vendorEffectiveTurnover")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal vendorEffectiveTurnover;

    @JsonProperty("vendorJackpotAmount")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal vendorJackpotAmount;

    @JsonProperty("vendorSettleTime")
    private Long vendorSettleTime;

    /** Vendor-mapper-owned signal; framework reads verbatim to flip ledger to SETTLED. */
    @JsonProperty("isRoundEnded")
    private Boolean isRoundEnded;

    // --- Refund-mode (state == REFUNDED) ---

    /** Vendor-side ids of the upstream DebitTxn(s) this credit refunds. Audit only. */
    @JsonProperty("referencedVendorDebitTxnIds")
    private List<String> referencedVendorDebitTxnIds;

    /** GA-side gaTxnIds resolved from {@link #referencedVendorDebitTxnIds}; used for cross-linking. */
    @JsonProperty("referencedDebitGaTxnIds")
    private List<String> referencedDebitGaTxnIds;

    public CreditTxn() {
        super();
    }

    /**
     * @param settleState must be {@link GameRoundState#SETTLED} or {@link GameRoundState#REFUNDED}.
     */
    public static CreditTxn of(String vendorClassName, String vendorTxnId, GameRoundState settleState, long createdTimestamp) {
        if (settleState != GameRoundState.SETTLED && settleState != GameRoundState.REFUNDED) {
            throw new IllegalArgumentException("CreditTxn state must be SETTLED or REFUNDED, got " + settleState);
        }
        CreditTxn txn = new CreditTxn();
        prepareAudit(txn, vendorClassName, vendorTxnId, createdTimestamp);
        txn.setState(settleState);
        return txn;
    }

    public static String createDocId(String vendorClassName, String vendorTxnId) {
        return vendorClassName + "::CREDIT::" + vendorTxnId;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return createDocId(getVendorClassName(), getVendorTxnId());
    }

    @JsonIgnore
    public boolean isRefund() {
        return isRefunded();
    }
}
