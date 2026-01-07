package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

@Component("pgsoftPromoPayoutRequestMapper")
public class PromoRequestMapper implements VendorRequestMapper<PromoPayoutContext, CashTransferInOutDto> {
    @Override
    public PromoPayoutContext toInternal(CashTransferInOutDto vendorRequest) {
        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorTransactionId(vendorRequest.getTransactionId())
                .vendorPlayerUsername(vendorRequest.getPlayerName())
                .vendorCurrency(vendorRequest.getCurrencyCode())
                // promo payout history
                .vendorCampaignCode(vendorRequest.getFreeGameId().toString())
                .vendorPayoutAmount(vendorRequest.getTransferAmount())
                .vendorTransactionTime(vendorRequest.getUpdatedTime())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
