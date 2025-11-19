package com.nextgen.gameaggregator.vendor.mg.api.promo;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.mg.api.betresult.UpdateBalanceDto;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutRequestMapper implements PromoPayoutContextMapper<UpdateBalanceDto> {
    @Override
    public PromoPayoutContext toInternal(UpdateBalanceDto vendorRequest) {

        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getTxnId())
                .vendorPlayerUsername(vendorRequest.getPlayerId())
                // promo payout history
                .vendorCampaignCode(vendorRequest.getMetaData().getFreeGame().getOfferGuid())
                .vendorTransactionId(vendorRequest.getTxnId())
                .vendorPayoutAmount(vendorRequest.getAmount())
                .vendorTransactionTime(vendorRequest.getCreationTimeMs())
                .vendorCurrency(vendorRequest.getCurrency())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
