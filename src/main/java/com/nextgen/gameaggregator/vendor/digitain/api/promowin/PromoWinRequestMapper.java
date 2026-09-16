package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Maps Digitain {@code /promowin} onto the internal promo-payout context.
 *
 * <p>Replaces a {@code BetResultContextMapper} that described the payout as a bet: zero stake, zero win,
 * the whole amount in {@code jackpotAmount}, and {@code roundId} set to the transaction id — a round that
 * never happened. Digitain's own spec is explicit that "these wins do not have a bet associated with them".
 *
 * <p><b>No campaign is resolved.</b> Digitain sends {@code pmid}, "Identifier of the promotion (Jackpot,
 * Tournament, etc.)", but {@code ga-promo-engine} has no Digitain integration — no campaign is created and
 * no grant provisioned — so there is nothing for {@code pmid} to match. Leaving both the resolve strategy
 * and {@code vendorCampaignCode} unset makes the enricher skip resolution instead of calling the promo
 * engine for a lookup that cannot succeed. That is also what makes {@link PromoType#JACKPOT} (id 6) safe
 * here: the {@code CampaignType} ceiling of 2 binds only payouts that actually resolve a campaign.
 */
@Component
public class PromoWinRequestMapper implements PromoPayoutContextMapper<PromoWinRequest> {

    /**
     * Digitain {@code opt} values that can arrive on {@code /promowin}.
     *
     * <p>The endpoint's own description is the filter: "These wins do not have a bet associated with
     * them". In the spec's Operation Types table that means the types carrying no Connected Bet ID —
     * {@code 26} Jackpot Win, {@code 72} Drop Win, {@code 75} Cashback, {@code 76} RealMoneyBonusReward,
     * {@code 85} TournamentPrizeWin (noted there as the "Company Tournament promowin operation type") and
     * {@code 27} Tip.
     *
     * <p>Paired win types are deliberately excluded even though they sound applicable: {@code 5} In-game
     * Gift Win is the win half of {@code 4} In-game Gift Bet, and {@code 35} Tournament Win the win half
     * of {@code 34} Tournament Bet. Both have a bet behind them, so they settle through {@code /bet} and
     * {@code /result}.
     *
     * <p>{@code 72} and {@code 76} are bet-less and so belong here, but their internal classification is
     * still pending; leaving them unmapped rejects them loudly rather than misfiling them. {@code 27} Tip
     * and {@code 75} Cashback are likewise unclassified and were not in the vendor's list.
     *
     * <p>{@link PromoType#JACKPOT} sits at id 6, above the promo-engine's {@code CampaignType} ceiling of
     * 2 — safe only because Digitain resolves no campaign, so {@code campaignType} is never sent.
     */
    private static final Map<String, PromoType> PROMO_TYPE_BY_OPERATION_TYPE = Map.of(
            "26", PromoType.JACKPOT,     // Jackpot Win
            "85", PromoType.TOURNAMENT); // TournamentPrizeWin

    @Override
    public PromoPayoutContext toInternal(PromoWinRequest vendorRequest) {
        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getTxid())
                .vendorTransactionId(vendorRequest.getTxid())
                .vendorPlayerUsername(vendorRequest.getPid())
                .vendorGameCode(vendorRequest.getGid())
                // cid is the player's currency, not a brand or channel id.
                .vendorCurrency(vendorRequest.getCid())
                .vendorPayoutAmount(vendorRequest.getPwa())
                .promoType(resolvePromoType(vendorRequest.getOpt()))
                .build();
    }

    /**
     * Rejects an unrecognised {@code opt} rather than defaulting one.
     *
     * <p>{@code opt} decides the {@code promo_type} written to {@code promo_payout_history} and the
     * classification sent to the operator, and with no campaign attached it is the <em>only</em>
     * description of what the credit was. Guessing would misfile a real payout silently, which is the
     * class of defect this ticket exists to fix.
     *
     * <p><b>Behaviour change worth watching in QA.</b> Every {@code opt} was previously accepted and
     * booked as a bet win, so anything outside {26, 85} is now refused — the unclassified bet-less types
     * ({@code 27}, {@code 72}, {@code 75}, {@code 76}) and the paired types that should never arrive here
     * ({@code 5}, {@code 35}). If production traffic shows any of them in use, map them rather than
     * removing the check: a paired type turning up would mean the endpoint is carrying settled bets.
     */
    static PromoType resolvePromoType(String operationType) {
        return Optional.ofNullable(operationType)
                .map(PROMO_TYPE_BY_OPERATION_TYPE::get)
                .orElseThrow(() -> new InvalidRequestException("unsupported promowin operation type: " + operationType));
    }
}
