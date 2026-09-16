package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Maps Aviatrix {@code /transactions/promoWin} onto the internal promo-payout context.
 *
 * <p>{@code promo.bonusId} is deliberately not carried onto the context. It fed
 * {@code vendorFreeRoundBonusId} for a {@code USERNAME_AND_BONUS_ID} campaign lookup that could never
 * match — see {@link AviatrixPromoPayoutService} — and nothing else reads it: neither the operator
 * request nor {@code promo_payout_history} has a field for a vendor-side promo reference. Mapping it
 * would only invite the same wrong assumption again.
 *
 * <p>{@code productId} is Aviatrix's game identifier and maps to {@code vendorGameCode}. It was
 * deliberately unmapped until ONEAPI-420, when carrying it would have triggered
 * {@code BaseEnricher.enrichVendorGame} and thrown {@code InternalConfigurationException} for a game
 * absent from {@code vendor_game} — indistinguishable from the player-lookup failure that throws the
 * same type, so neither could be mapped to Aviatrix's distinct {@code Product not found} and
 * {@code Player not found} responses. That no longer applies: {@code PromoPayoutContext} is still not
 * {@code VendorGameAware}, so {@code enrichVendorGame} stays a no-op, and
 * {@code PromoPayoutContextEnricher.populateGame} resolves the game itself — logging
 * {@code [PROMO_GAME_UNRESOLVED]} and leaving {@code gameCode} unset rather than failing the payout.
 *
 * <p>That tag should never fire for this vendor: {@code PromoWinAction.doVerification} already rejects
 * an unknown product up front with {@code Product not found}, before enrichment runs.
 *
 * <p>Expect benign {@code [PROMO_GAME_MISMATCH]} warnings. As {@code doVerification} documents, a promo
 * can arrive while the player's newest session belongs to a different game; the check logs the
 * difference and forwards the request's game, which is the one the prize was won in.
 */
@Component
public class PromoWinRequestMapper implements PromoPayoutContextMapper<PromoWinDto> {

    static final String TYPE_BONUS = "bonus";
    static final String TYPE_TOURNAMENT = "tournament";

    /**
     * Aviatrix promo types, per the {@code /transactions/promoWin} spec.
     *
     * <p>An unrecognised type is rejected rather than defaulted: {@code promo.type} decides both the
     * campaign-resolution strategy and the {@code promo_type} written to {@code promo_payout_history},
     * so guessing would silently misclassify a real payout.
     */
    private static final Map<String, PromoType> PROMO_TYPE_BY_VENDOR_TYPE = Map.of(
            TYPE_BONUS, PromoType.FREE_ROUND,
            TYPE_TOURNAMENT, PromoType.TOURNAMENT);

    @Override
    public PromoPayoutContext toInternal(PromoWinDto vendorRequest) {
        PromoDto promo = vendorRequest.getPromo();
        PromoType promoType = resolvePromoType(promo.getType());

        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getTxId())
                .vendorTransactionId(vendorRequest.getTxId())
                .vendorPlayerUsername(vendorRequest.getPlayerId())
                .vendorSessionToken(vendorRequest.getSessionToken())
                .vendorCurrency(vendorRequest.getCurrency())
                .vendorPayoutAmount(vendorRequest.getPayoutAmount())
                .vendorGameCode(vendorRequest.getProductId())
                .promoType(promoType)
                .build();
    }

    /**
     * Resolves the internal {@link PromoType} for an Aviatrix {@code promo.type}.
     *
     * <p>Aviatrix {@code bonus} maps to {@link PromoType#FREE_ROUND}, not {@link PromoType#BONUS}. Bonus is
     * a Back Office campaign type but is absent from the promoType values the Operator promo-payout
     * contract enumerates (ONEAPI-106), and {@code PromoPayoutMapper} forwards {@code code} verbatim — an
     * Operator validating the field would reject {@code BONUS} and the payout would fail. The cost is
     * borne by reporting, not the Operator: a bonus payout is written to {@code promo_payout_history} as
     * Free Round (id 1). Revisit if {@code BONUS} is added to the contract.
     *
     * <p>Both mapped ids sit within the promo-engine's {@code CampaignType} ceiling of 2, so neither would
     * break if a {@code vendorCampaignCode} were attached here later and put the payout on the default
     * campaign-resolution path. Today Aviatrix resolves no campaign at all — {@code campaignType} is never
     * sent.
     */
    static PromoType resolvePromoType(String vendorType) {
        return Optional.ofNullable(vendorType)
                .map(PROMO_TYPE_BY_VENDOR_TYPE::get)
                .orElseThrow(() -> new InvalidRequestException("unsupported promo.type: " + vendorType));
    }
}
