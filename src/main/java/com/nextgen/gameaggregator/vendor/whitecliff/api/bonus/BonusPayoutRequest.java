package com.nextgen.gameaggregator.vendor.whitecliff.api.bonus;

import lombok.Builder;
import lombok.Getter;

/**
 * A {@code /bonus} request paired with the values only the game session can supply.
 *
 * <p>{@link BonusDto} carries no vendor player username — its {@code user_id} is matched against
 * {@code GameSession.vendorToken}, not against a username — and no currency. Both are needed to build a
 * {@code PromoPayoutContext}, and {@code PromoPayoutContextMapper.toInternal} only ever sees the request
 * object, so they are bundled here rather than reached for inside the mapper. Same shape as Habanero's
 * {@code HabaneroBonusPayoutRequest}.
 */
@Getter
@Builder
public class BonusPayoutRequest {

    private final BonusDto bonus;

    /** From {@code GameSession.vendorPlayerUsername} — the key the promo engine resolves the player by. */
    private final String vendorPlayerUsername;

    /** From {@code GameSession.vendorCurrencyCode}. */
    private final String vendorCurrency;
}
