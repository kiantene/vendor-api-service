package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.context.VendorAwareContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal context for a wallet-credit inbound request from a vendor that speaks
 * debit/credit on the wire. Handles BOTH settlement and refund/rollback — the
 * {@link #isRefund} discriminator selects the mode.
 *
 * <p>Field-name convention:
 * <ul>
 *   <li>{@code vendor*} — from the vendor wire (pre-FX amounts, vendor-native ids/times).</li>
 *   <li>{@code is*} — boolean flags ({@link #isRefund}, {@link #isRoundEnded}).</li>
 *   <li>{@code referencedVendorDebitTxnIds} — vendor ids of the upstream debits this credit closes (both modes).</li>
 * </ul>
 *
 * <p>Field-population rules per mode:
 * <ul>
 *   <li>{@code isRefund = false} (settle): populates {@code vendorWinAmount, vendorWinLoss,
 *       vendorEffectiveTurnover, vendorJackpotAmount, vendorBetTime, vendorSettleTime,
 *       isRoundEnded}, and {@code referencedVendorDebitTxnIds} when the vendor opts into
 *       {@code CreditConfig.requireDebitReference}, and whenever the vendor sends them.</li>
 *   <li>{@code isRefund = true} (refund): populates {@code referencedVendorDebitTxnIds};
 *       settle-mode analytics stay null.</li>
 * </ul>
 *
 * <p>Field names use <strong>System / operator vocabulary</strong>, not vendor-wire shorthand
 * — e.g. {@code vendorWinLoss} maps to the operator's {@code winLoss};
 * {@code vendorEffectiveTurnover} maps to the operator's {@code effectiveTurnover}.
 * The per-vendor {@link CreditContextMapper} translates from each vendor wire's naming.
 * Vendor-only audit fields that never reach the operator wire or BetHistory kafka payload
 * deliberately do not appear on this context — keep the framework vocabulary minimal.
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class CreditContext extends VendorAwareContext {

    /** Vendor's transaction id verbatim for this credit, from the wire. */
    private String vendorTxnId;

    /** Vendor's round id. */
    private String vendorRoundId;

    /**
     * Credit amount in vendor currency (pre-FX) — the wallet movement (what the player receives).
     * For settle, the payout. For refund, the refunded value. Distinct from {@link #vendorBetAmount}.
     * Always positive.
     */
    private BigDecimal vendorAmount;

    /**
     * Original bet amount in vendor currency (pre-FX) — the wager being settled/refunded.
     * Analytics passthrough for operator reconciliation.
     */
    private BigDecimal vendorBetAmount;

    /** Vendor-side timestamp for the credit, epoch ms. */
    private Long vendorTimestamp;

    /** First-class discriminator: false = settlement, true = refund/rollback. */
    private boolean isRefund;

    // --- Settle-mode passthrough (isRefund == false) ---

    private BigDecimal vendorWinAmount;

    /**
     * Win/Loss from the player's perspective (may be negative). Maps to the operator wire's
     * required {@code winLoss} field.
     *
     * <p><strong>Vendor mapper sets this when the wire provides it explicitly.</strong>
     * When the wire doesn't carry it (the common case across debit/credit vendors),
     * leave this field {@code null} and use {@link #resolveVendorWinLoss()} to read it —
     * the framework falls back to {@code vendorWinAmount - vendorBetAmount}.
     *
     * <p>Read this field directly only when you specifically want the vendor-supplied raw
     * value (e.g. audit reconciliation against the vendor wire). All downstream consumers
     * (operator wire, BetHistory kafka, entity persistence) use the resolved value.
     */
    private BigDecimal vendorWinLoss;

    /**
     * Effective turnover for rollover/rebate. Maps to the operator wire's required
     * {@code effectiveTurnover} field.
     *
     * <p><strong>Vendor mapper sets this when the wire provides it explicitly.</strong>
     * When the wire doesn't carry it, leave this field {@code null} and use
     * {@link #resolveVendorEffectiveTurnover()} to read it — the framework falls back to
     * {@code vendorBetAmount} (turnover ≡ wager).
     */
    private BigDecimal vendorEffectiveTurnover;

    private BigDecimal vendorJackpotAmount;
    private Long vendorBetTime;
    private Long vendorSettleTime;

    /** Vendor-mapper-owned signal. Framework reads verbatim to flip ledger to SETTLED on success. */
    private Boolean isRoundEnded;

    // --- Debit linkage (both modes) ---

    /**
     * Vendor-side ids of the upstream debits this credit closes, as {@code DebitTxn} doc-id suffixes.
     * <ul>
     *   <li><b>Refund</b> — required: empty is rejected ({@code BetNotFoundException}), and each
     *       named debit is checked like a settle's.</li>
     *   <li><b>Settle</b> — always checked when present. Required only when
     *       {@code CreditConfig.requireDebitReference} is on; otherwise a settle naming none
     *       proceeds unchecked. So a mapper that forgets to populate this fails loudly for a vendor
     *       that requires it, but silently skips the double-spend guard for one that does not.</li>
     * </ul>
     */
    private List<String> referencedVendorDebitTxnIds;

    /**
     * Returns the effective {@code winLoss} for downstream consumers — operator wire,
     * BetHistory kafka, entity persistence. Falls back to {@code vendorWinAmount - vendorBetAmount}
     * (treating nulls as zero) when {@link #vendorWinLoss} is {@code null}. Refund-mode callers
     * already zero this out before this is read.
     */
    public BigDecimal resolveVendorWinLoss() {
        if (vendorWinLoss != null) return vendorWinLoss;
        BigDecimal win = vendorWinAmount != null ? vendorWinAmount : BigDecimal.ZERO;
        BigDecimal bet = vendorBetAmount != null ? vendorBetAmount : BigDecimal.ZERO;
        return win.subtract(bet);
    }

    /**
     * Returns the effective {@code effectiveTurnover} for downstream consumers. Falls back
     * to {@link #vendorBetAmount} (turnover ≡ wager) when {@link #vendorEffectiveTurnover}
     * is {@code null}.
     */
    public BigDecimal resolveVendorEffectiveTurnover() {
        return vendorEffectiveTurnover != null ? vendorEffectiveTurnover : vendorBetAmount;
    }
}
