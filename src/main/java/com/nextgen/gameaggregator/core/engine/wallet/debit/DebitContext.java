package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.context.VendorAwareContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Internal context for a wallet-debit inbound request from a vendor that speaks
 * debit/credit on the wire.
 * <p>
 * A debit is a wallet movement, not a bet. The vendor's wire id is a {@code vendorTxnId}
 * (carried verbatim from the wire layer by the per-vendor {@link DebitContextMapper}).
 * <p>
 * Field-name convention:
 * <ul>
 *   <li>{@code vendor*}     — from the vendor wire (pre-FX amounts, vendor-native ids/times).</li>
 *   <li>{@code idempotencyKey} — platform-internal dedupe key, often equal to {@link #vendorTxnId}.</li>
 * </ul>
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class DebitContext extends VendorAwareContext {

    /** Vendor's transaction id verbatim from the wire. Often identical to {@link #getIdempotencyKey()}. */
    private String vendorTxnId;

    /** Vendor's round id — drives wallet-round grouping ({@code vendorClassName::vendorPlayerUsername::vendorRoundId}). */
    private String vendorRoundId;

    /** Debit amount in vendor currency (pre-FX), always positive. */
    private BigDecimal vendorAmount;

    /** Vendor-side timestamp for the debit, epoch ms. */
    private Long vendorTimestamp;
}
