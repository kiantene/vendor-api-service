package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.tournament;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import org.springframework.stereotype.Component;

@Component
public class TournamentPayoutRequestMapper implements PromoPayoutContextMapper<TournamentPayoutRequest> {
    @Override
    public PromoPayoutContext toInternal(TournamentPayoutRequest vendorRequest) {
        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getProviderId())
                .vendorPlayerUsername(vendorRequest.getUserId())
//                .vendorCurrency(vendorRequest.getCurrencyCode())
//                .vendorGameCode(vendorRequest.getGameId())
                // promo payout history
                .vendorCampaignCode(vendorRequest.getCampaignId())
                .vendorTransactionId(vendorRequest.getProviderId())
                .vendorPayoutAmount(vendorRequest.getAmount())
                .vendorTransactionTime(vendorRequest.getTimestamp())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
