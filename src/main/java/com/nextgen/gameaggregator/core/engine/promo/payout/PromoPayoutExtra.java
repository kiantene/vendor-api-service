package com.nextgen.gameaggregator.core.engine.promo.payout;

import lombok.Data;

/**
 * Supplementary identifiers on a promo payout, nested under {@code extra} in the operator request.
 *
 * <p>Requested by the transfer wallet, which resolves a player from the encoded {@code username} and
 * wants the raw id alongside it. Kept as its own object rather than flattened onto
 * {@link PromoPayoutDto} so later additions do not widen the top-level contract every operator parses.
 *
 * <p>Compare {@code RollbackMeta}, the equivalent on wallet rollback: that one is stripped before it
 * can reach a real operator. Whether {@code extra} needs the same treatment is a contract decision -
 * if it does, the strip belongs in {@code ClientRequestService} beside the rollback one, not here.
 */
@Data
public class PromoPayoutExtra {

    /** The agent-side player id, the same player the DTO's {@code username} names. */
    private String playerId;
}
