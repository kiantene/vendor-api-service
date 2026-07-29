package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

/**
 * Evolution v2 promo-payout integration.
 */
@Component
public class PromoPayoutRequestMapper implements PromoPayoutContextMapper<PromoPayoutRequestDto> {

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
                .promoType(PromoType.FREE_ROUND)
                .vendorRequestUuid(vendorRequest.getUuid())
                .build();
    }
}
