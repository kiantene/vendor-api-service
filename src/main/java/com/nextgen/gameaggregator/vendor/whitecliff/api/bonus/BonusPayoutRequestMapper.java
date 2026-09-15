package com.nextgen.gameaggregator.vendor.whitecliff.api.bonus;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

/**
 * Maps WhiteCliff {@code /bonus} onto the internal promo-payout context.
 *
 * <p><b>No campaign.</b> {@code BonusDto} carries no campaign or bonus reference, so
 * {@code vendorCampaignCode} and {@code vendorFreeRoundBonusId} are both left unset. With no resolve
 * strategy configured either, {@code PromoPayoutContextEnricher.populateCampaign} returns early and no
 * campaign is attached — which is what makes the promo types below safe: {@code BONUS} (5) and
 * {@code JACKPOT} (6) both sit above the promo engine's {@code CampaignType} ceiling of 2, and that
 * ceiling binds only requests that actually resolve a campaign. Attaching a campaign key here later
 * would put these on the default resolution path and the lookup would match nothing.
 *
 * <p>Amounts need no scaling: WhiteCliff quotes {@code amount} in major units as a {@code BigDecimal},
 * unlike vendors that use minor units.
 */
@Component
public class BonusPayoutRequestMapper implements PromoPayoutContextMapper<BonusPayoutRequest> {

    /** {@code 0} — In Game Bonus / 普通奖金. Won during play, so it stays on the bet-result flow. */
    public static final BigInteger TYPE_IN_GAME_BONUS = BigInteger.ZERO;

    /**
     * WhiteCliff's {@code type} values that are promo payouts rather than gameplay winnings.
     *
     * <p>{@code 0} (In Game Bonus) is deliberately absent — it is won inside a game round and belongs in
     * bet history, so {@code BonusAction} keeps routing it through {@code processBetResult}. Only
     * {@code 1} (Promotion / 活动奖金) and {@code 2} (Jackpot / 派彩奖金) are the free, non-billable
     * payouts ONEAPI-358 is about.
     *
     * <p>{@code 1} maps to {@link PromoType#BONUS} because the internal enum has no "Promotion" member:
     * {@code MISSION} carries a warning against mapping vendor types onto it without confirming
     * semantics, and {@code PRIZEDROP} names a specific mechanic rather than a general promotion.
     */
    private static final Map<BigInteger, PromoType> PROMO_TYPE_BY_VENDOR_TYPE = Map.of(
            BigInteger.ONE, PromoType.BONUS,     // Promotion
            BigInteger.TWO, PromoType.JACKPOT);  // Jackpot

    /** Whether this bonus type is paid out as a promo rather than booked as a bet win. */
    public static boolean isPromoPayout(BigInteger vendorType) {
        return vendorType != null && PROMO_TYPE_BY_VENDOR_TYPE.containsKey(vendorType);
    }

    @Override
    public PromoPayoutContext toInternal(BonusPayoutRequest vendorRequest) {
        BonusDto bonus = vendorRequest.getBonus();

        return PromoPayoutContext.builder()
                .idempotencyKey(bonus.getTxnId())
                .vendorTransactionId(bonus.getTxnId())
                .vendorPlayerUsername(vendorRequest.getVendorPlayerUsername())
                .vendorCurrency(vendorRequest.getVendorCurrency())
                .vendorSessionToken(bonus.getSid())
                .vendorPayoutAmount(bonus.getAmount())
                .promoType(resolvePromoType(bonus.getType()))
                .build();
    }

    /**
     * Rejects an unrecognised {@code type} rather than defaulting one. It decides the
     * {@code promo_type} written to {@code promo_payout_history} and the classification forwarded to the
     * operator, so guessing would silently misfile a real payout.
     */
    static PromoType resolvePromoType(BigInteger vendorType) {
        return Optional.ofNullable(vendorType)
                .map(PROMO_TYPE_BY_VENDOR_TYPE::get)
                .orElseThrow(() -> new InvalidRequestException("unsupported bonus type: " + vendorType));
    }
}
