package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.core.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

/**
 * Framework-level validation for the credit (settle + refund) path.
 *
 * <p>Unlike {@link com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetValidator
 * WalletBetValidator}, there is deliberately <b>no session-derived eligibility check</b> (session
 * active, vendor line / agent player / game / currency ACTIVE), for either mode. A credit closes a
 * debit that already happened; whether the session has since been terminated or the player
 * disabled says nothing about whether that debit should be closed, and refusing it strands the
 * round with the money already taken (OVI-2156: Action 10 failed with 9999 after termination while
 * Action 11 succeeded). Player status is the operator's to enforce on the wallet call itself, so a
 * pre-check here could only refuse a credit the operator would have accepted. Refund has always
 * skipped it, matching the legacy {@code CancelBetService}, which never called
 * {@code validateEligibleBet}.
 */
@Service
class WalletCreditValidator {

    /**
     * Request-shape check. Both assertions are preconditions of the pipeline that follows:
     * {@code vendorPlayerUsername} keys the enrichment chain and the replay vendor view, and
     * {@code vendorTxnId} is half the idempotency doc id
     * ({@link com.nextgen.gameaggregator.entity.couchbase.CreditTxn#createDocId}).
     *
     * <p>A blank {@code vendorTxnId} would collapse every such request onto the single doc id
     * {@code <vendor>::CREDIT::null}, so with {@code returnSuccessOnDuplicate} enabled one player's
     * settle would replay another player's recorded balance. Enforced here rather than per vendor
     * because the doc-id contract is the framework's, not the vendor's.
     */
    public void validateRequestContext(CreditContext context) {
        if (context.getVendorPlayerUsername() == null) {
            throw new InvalidRequestException("Username cannot be empty");
        }
        if (context.getVendorTxnId() == null || context.getVendorTxnId().isBlank()) {
            throw new InvalidRequestException("Transaction id cannot be empty");
        }
    }
}
