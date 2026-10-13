package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.core.validator.BetValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Framework-level validation for the debit (wallet-movement) path.
 * Mirrors {@link com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetValidator
 * WalletBetValidator} — request shape check + post-enrichment session/eligibility check via
 * the shared {@link BetValidator}.
 */
@Service
@RequiredArgsConstructor
class WalletDebitValidator {

    private final BetValidator validator;

    /**
     * Request-shape check. Both assertions are preconditions of the pipeline that follows:
     * {@code vendorPlayerUsername} keys the enrichment chain and the replay vendor view, and
     * {@code vendorTxnId} is half the idempotency doc id
     * ({@link com.nextgen.gameaggregator.entity.couchbase.DebitTxn#createDocId}).
     *
     * <p>A blank {@code vendorTxnId} would collapse every such request onto the single doc id
     * {@code <vendor>::DEBIT::null}, so with {@code returnSuccessOnDuplicate} enabled one player's
     * debit would replay another player's recorded balance. Enforced here rather than per vendor
     * because the doc-id contract is the framework's, not the vendor's.
     */
    public void validateRequestContext(DebitContext context) {
        if (context.getVendorPlayerUsername() == null) {
            throw new InvalidRequestException("Username cannot be empty");
        }
        if (context.getVendorTxnId() == null || context.getVendorTxnId().isBlank()) {
            throw new InvalidRequestException("Transaction id cannot be empty");
        }
    }

    public void validateBusinessState(GameSession session, DebitContext context)
            throws GameSessionExpiredException, GameTerminatedException,
            PlayerDisabledException, BetNotAllowedException {

        validator.validateBusinessState(session, context.getVendorPlayerUsername(), context);
    }
}
