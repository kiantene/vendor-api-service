package com.nextgen.gameaggregator.vendor.jili.api.freespin;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

@Component
public class JiliFreeSpinPayoutRequestMapper implements PromoPayoutContextMapper<JiliFreeSpinPayoutRequest> {

    @Override
    public PromoPayoutContext toInternal(JiliFreeSpinPayoutRequest request) {
        return PromoPayoutContext.builder()
                .idempotencyKey(request.getReqId())
                .campaignUuid(request.getFreeSpinData().getReferenceId())
                .vendorCampaignCode(request.getFreeSpinData().getReferenceId())
                .promoType(PromoType.FREE_ROUND)
                .vendorTransactionId(request.getRound())
                .vendorPayoutAmount(request.getWinloseAmount())
                .vendorPlayerUsername(request.getVendorPlayerUsername())
                .vendorCurrency(request.getVendorCurrencyCode())
                .token(request.getToken())
                .vendorTransactionTime(request.getWagersTime().longValueExact() * 1000L)
                .build();
    }
}
