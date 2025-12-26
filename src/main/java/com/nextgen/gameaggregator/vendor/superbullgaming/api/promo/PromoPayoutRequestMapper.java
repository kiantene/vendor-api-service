package com.nextgen.gameaggregator.vendor.superbullgaming.api.promo;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.superbullgaming.api.betNSettle.BetNSettleDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class PromoPayoutRequestMapper implements PromoPayoutContextMapper<BetNSettleDto> {

    private static final Map<Integer, PromoType> PROMO_TYPE_MAP = Map.of(
            1, PromoType.FREE_ROUND,
            2, PromoType.PRIZEDROP,
            3, PromoType.TOURNAMENT);

    @Override
    public PromoPayoutContext toInternal(BetNSettleDto vendorRequest) {
        PromoType promoType = Optional.of(PROMO_TYPE_MAP.get(vendorRequest.getPromoType()))
                .orElseThrow(() -> new InvalidRequestException("Invalid promo type: " + vendorRequest.getPromoType()));

        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getTraceId())
                .vendorPlayerUsername(vendorRequest.getUsername())
                .vendorCurrency(vendorRequest.getCurrency())
                .vendorCampaignCode(vendorRequest.getPromoCode())
                .vendorTransactionId(vendorRequest.getBetId())
                .vendorPayoutAmount(vendorRequest.getPayout())
                .vendorTransactionTime(vendorRequest.getProcessedTime())
                .promoType(promoType)
                .build();
    }
}
