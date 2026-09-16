package com.nextgen.gameaggregator.vendor.hacksaw.api.promopayout;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HacksawPromoPayoutRequestMapper implements PromoPayoutContextMapper<PromoPayoutDto> {

    @Override
    public PromoPayoutContext toInternal(PromoPayoutDto request) {
        return PromoPayoutContext.builder()
                .idempotencyKey(String.valueOf(request.getPromoPayoutId()))
                .campaignUuid(request.getExternalPromoId())
                .vendorCampaignCode(request.getExternalPromoId())
                .promoType(PromoType.FREE_ROUND)
                .vendorTransactionId(String.valueOf(request.getPromoPayoutId()))
                .vendorPayoutAmount(BigDecimal.valueOf(request.getAmount()))
                .vendorPlayerUsername(request.getExternalPlayerId())
                .vendorCurrency(request.getCurrency())
                .vendorGameCode(request.getGameId())
                .build();
    }
}
