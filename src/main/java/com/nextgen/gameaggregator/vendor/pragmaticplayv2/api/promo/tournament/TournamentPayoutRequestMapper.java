package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.tournament;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

@Component
public class TournamentPayoutRequestMapper implements PromoPayoutContextMapper<TournamentPayoutRequest> {
    @Override
    public PromoPayoutContext toInternal(TournamentPayoutRequest vendorRequest) {
        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getReference())
                .vendorPlayerUsername(vendorRequest.getUserId())
                .vendorCurrency(vendorRequest.getCurrency())
//                .vendorGameCode(vendorRequest.getGameId())
                // promo payout history
                .vendorCampaignCode(vendorRequest.getCampaignId())
                .vendorTransactionId(vendorRequest.getProviderId())
                .vendorPayoutAmount(vendorRequest.getAmount())
                .vendorTransactionTime(vendorRequest.getTimestamp())
                .promoType(PromoType.TOURNAMENT)
                .build();
    }
}
