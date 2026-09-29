package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutMapper {
    public PromoPayoutDto toPromoPayoutRequest(PromoPayoutContext context) {
        if (context == null) {
            return null;
        }

        return PromoPayoutDto.builder()
                .traceId(context.getTraceId())
                .username(context.getAgent().playerUsername())
                .transactionId(context.getTransactionId())
                .campaignId(context.getCampaignUuid())
                .promoType(context.getPromoType().code)
                .gameCode(operatorGameCode(context))
                .currency(context.getCurrencyCode())
                .amount(context.getPayout().amount())
                .timestamp(context.getVendorTransactionTime())
                .extra(extra(context))
                .build();
    }

    public PromoPayoutDto toPromoPayoutRequest(PromoPayoutContext context, PayoutTransaction txn) {
        if (context == null) {
            return null;
        }

        return PromoPayoutDto.builder()
                .traceId(txn.getTraceId())
                .username(context.getAgent().playerUsername()) // TODO: potential issue if txn username != context username
                .transactionId(txn.getTransactionId())
                .campaignId(context.getCampaignUuid())
                // promoType lives on the context; batch children do not carry their own
                .promoType(context.getPromoType().code)
                .gameCode(operatorGameCode(context))
                .currency(context.getCurrencyCode())
                .amount(txn.getPayout().amount())
                .timestamp(txn.getVendorTransactionTime())
                .extra(extra(context))
                .build();
    }

    /**
     * The {@code extra} block, or null when there is nothing to put in it - an absent player id leaves
     * the key out of the payload rather than sending {@code "extra": {"playerId": null}}.
     *
     * <p>Batch children share the context's player: the batch overload already takes
     * {@code username} from the context for the same reason.
     */
    private PromoPayoutExtra extra(PromoPayoutContext context) {
        Long playerId = context.getAgent().playerId();
        if (playerId == null) {
            return null;
        }
        PromoPayoutExtra extra = new PromoPayoutExtra();
        extra.setPlayerId(String.valueOf(playerId));
        return extra;
    }

    /**
     * The operator receives {@code gameCode} only for {@link PromoType#FREE_ROUND}. Every other promo
     * type sends it absent, even when the vendor did supply a game.
     *
     * <p>Deliberately <b>not</b> applied to {@code promo_payout_history.game_code}, which keeps the game
     * for every promo type that supplies one. The two sinks therefore disagree by design: a tournament
     * payout can carry a game code in ClickHouse while omitting it on the operator request. Do not
     * change one to match the other.
     *
     * <p>{@code promoType} is non-null here — {@code PromoPayoutValidator} rejects a context without one
     * before the processor runs.
     */
    private String operatorGameCode(PromoPayoutContext context) {
        return context.getPromoType() == PromoType.FREE_ROUND ? context.getGameCode() : null;
    }
}
