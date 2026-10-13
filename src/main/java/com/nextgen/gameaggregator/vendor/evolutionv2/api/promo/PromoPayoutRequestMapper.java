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
     * <p>The type decides how the campaign is resolved: free rounds are ours and are looked up by voucher
     * ({@code playerUuidCampaignLookup}), while every type here is Evolution-run and goes to
     * {@code createPayoutCampaign}, keyed on the vendor's own campaign id where the type carries one.
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
        PromoType promoType = resolvePromoType(transaction.getType());
        boolean freeRound = promoType == PromoType.FREE_ROUND;

        return EvolutionPromoPayoutContext.builder()
                .idempotencyKey(transaction.getId())
                .vendorSessionToken(vendorRequest.getSid())
                .vendorPlayerUsername(vendorRequest.getUserId())
                .vendorCurrency(vendorRequest.getCurrency())
                // Free round: voucherId identifies the campaign-player allocation. Since PromoPayoutAction
                // enables playerUuidCampaignLookup, the enricher interprets it as campaign_players.uuid.
                // Anything else is created from the payout, so it carries the vendor's campaign id instead -
                // a per-player voucherId there would mint a campaign per player. Null when the type has
                // none, and the promo engine then derives one campaign per payout.
                .vendorCampaignCode(freeRound ? voucherId : transaction.resolveCampaignId())
                .vendorCampaignName(freeRound ? null : transaction.getCampaignCode())
                .vendorTransactionId(transaction.getId())
                .vendorPayoutAmount(transaction.getAmount())
                .promoType(promoType)
                .vendorRequestUuid(vendorRequest.getUuid())
                .vendorGameCode(vendorRequest.getGame() == null ? null : vendorRequest.getGame().getId())
                .build();
    }

    private static PromoType resolvePromoType(String transactionType) {
        if (transactionType == null) {
            return PromoType.FREE_ROUND;
        }
        return PROMO_TYPE_BY_TRANSACTION_TYPE.getOrDefault(transactionType, PromoType.FREE_ROUND);
    }
}
