package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DotConnectionsFreeSpinResultRequestMapper implements PromoPayoutContextMapper<FreeSpinResultDto> {

    /**
     * {@code wager_id} is the idempotency key, not {@code round_id}. A DCS round can settle in more than
     * one wager (§3.6 carries {@code is_endround} precisely because a round may span several results),
     * so keying on the round would collapse distinct wins in the same round into a single payout.
     *
     * <p>{@code vendorCampaignCode} is deliberately left unset. DCS campaign create is {@code LOCAL_ONLY}
     * in the promo engine, so a campaign has no vendor-side id — {@code vendorCampaignCode} holds the
     * campaign UUID, which DCS has never seen and cannot send. What DCS does send is {@code freespin_id}:
     * the grant {@code createFreeSpin} minted for one registration, recorded per player as
     * {@code CampaignPlayer.ext_info.vendorGrantRef}. Hence the mapping to {@code vendorFreeRoundBonusId}
     * and the {@code USERNAME_AND_BONUS_ID} strategy on the handler — resolution is (player, grant), not
     * (vendor line, campaign code).
     */
    @Override
    public PromoPayoutContext toInternal(FreeSpinResultDto request) {
        // Tripwire, not a guard: crediting every result is correct only while every result is terminal.
        // See FreeSpinResultDto#isEndround. Deliberately does not alter the payout — a partial that we
        // silently skipped would under-pay just as wrongly as one we double-counted.
        if (!"true".equalsIgnoreCase(request.getIsEndround())) {
            log.warn("DCS freeSpinResult with is_endround={} (wager {}, freespin {}) — expected every free "
                            + "spin to settle in one callback. If these carry partial amounts the round is being over-credited.",
                    request.getIsEndround(), request.getWagerId(), request.getFreespinId());
        }

        return PromoPayoutContext.builder()
                .idempotencyKey(request.getWagerId())
                .vendorTransactionId(request.getWagerId())
                .vendorPlayerUsername(request.getBrandUid())
                .vendorCurrency(request.getCurrency())
                .vendorGameCode(request.getGameId())
                .vendorFreeRoundBonusId(request.getFreespinId().toString())
                .vendorPayoutAmount(request.getAmount())
                .vendorTransactionTime(request.getTransactionTimeMillis())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
