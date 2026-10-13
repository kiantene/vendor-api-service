package com.nextgen.gameaggregator.core.engine.wallet.credit;

import lombok.Data;

/**
 * Per-request configuration for the credit pipeline.
 * Mirrors {@link com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig} at a smaller surface.
 */
@Data
public class CreditConfig {
    /** When true, a duplicate credit returns the prior balance as success instead of throwing. */
    private boolean returnSuccessOnDuplicate;

    /**
     * When false, a credit on a round whose ledger is already in {@code isEnded == true}
     * is rejected before the operator call. JDB requires this gate; default vendors allow
     * post-end credits (e.g., late refunds against a settled round).
     */
    private boolean allowCreditWhenRoundEnded;

    /**
     * When true, every credit must name the debits it closes in {@code referencedVendorDebitTxnIds};
     * one naming none is rejected as a malformed request. For a vendor whose protocol always sends
     * the reference (JDB {@code refTransferIds}). Named debits are checked either way; see
     * {@link DebitReferenceGuard}. Default false: a vendor with standalone credits (bonus, jackpot)
     * can send none.
     */
    private boolean requireDebitReference;

    public CreditConfig() {
        this.returnSuccessOnDuplicate = false;
        this.allowCreditWhenRoundEnded = true;
        this.requireDebitReference = false;
    }

    public CreditConfig returnSuccessOnDuplicate(boolean flag) {
        this.returnSuccessOnDuplicate = flag;
        return this;
    }

    public CreditConfig allowCreditWhenRoundEnded(boolean flag) {
        this.allowCreditWhenRoundEnded = flag;
        return this;
    }

    public CreditConfig requireDebitReference(boolean flag) {
        this.requireDebitReference = flag;
        return this;
    }
}
