package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evolution v2 promo-payout integration.
 */
@Component
public class PromoPayoutRequestMapper implements PromoPayoutContextMapper<PromoPayoutRequestDto> {

    /**
     * Evolution promo transaction types that are not free rounds. Anything absent falls back to
     * {@link PromoType#FREE_ROUND} — the free-round and reward-game families cover several concrete type
     * names (e.g. {@code FreeRoundPlayableSpent}, {@code RewardGame*}, {@code SmartSpinsMonetaryReward}),
     * and an unrecognised type must not fail a payout that would otherwise succeed.
     *
     * <p>Safe to reclassify because {@code PromoPayoutAction} enables {@code playerUuidCampaignLookup},
     * so {@code promoType} is never used as a campaign lookup key for this vendor.
     */
    private static final Map<String, PromoType> PROMO_TYPE_BY_TRANSACTION_TYPE = Map.of(
            "SmartTournamentMonetaryReward", PromoType.TOURNAMENT,
            "RtrMonetaryReward", PromoType.TOURNAMENT,
            "JackpotWin", PromoType.JACKPOT,
            "CashReward", PromoType.BONUS);

    @Override
    public PromoPayoutContext toInternal(PromoPayoutRequestDto vendorRequest) {
        PromoTransactionDto transaction = vendorRequest.getPromoTransaction();
        String voucherId = transaction.getVoucherId();
        if (voucherId != null) {
            voucherId = voucherId.replace("-", "");
        }

        return EvolutionPromoPayoutContext.builder()
                .idempotencyKey(transaction.getId())
                .vendorSessionToken(vendorRequest.getSid())
                .vendorPlayerUsername(vendorRequest.getUserId())
                .vendorCurrency(vendorRequest.getCurrency())
                // Evolution voucherId identifies the campaign-player allocation. Since
                // PromoPayoutAction enables playerUuidCampaignLookup, the enricher interprets
                // this value as campaign_players.uuid rather than a literal vendor campaign code.
                .vendorCampaignCode(voucherId)
                .vendorTransactionId(transaction.getId())
                .vendorPayoutAmount(transaction.getAmount())
                .promoType(resolvePromoType(transaction.getType()))
                .vendorRequestUuid(vendorRequest.getUuid())
                .build();
    }

    private static PromoType resolvePromoType(String transactionType) {
        if (transactionType == null) {
            return PromoType.FREE_ROUND;
        }
        return PROMO_TYPE_BY_TRANSACTION_TYPE.getOrDefault(transactionType, PromoType.FREE_ROUND);
    }
}
