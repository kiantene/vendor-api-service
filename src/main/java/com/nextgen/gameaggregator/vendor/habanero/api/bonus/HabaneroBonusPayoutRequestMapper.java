package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import org.springframework.stereotype.Component;

@Component
public class HabaneroBonusPayoutRequestMapper implements PromoPayoutContextMapper<HabaneroBonusPayoutRequest> {

    @Override
    public PromoPayoutContext toInternal(HabaneroBonusPayoutRequest request) {
        var fundTransferRequest = request.getFundTransferRequest();
        var fundInfo = request.getFundInfo();
        var bonusDetails = request.getBonusDetails();

        return PromoPayoutContext.builder()
                .idempotencyKey(fundInfo.getTransferId())
                .vendorCampaignCode(bonusDetails.getCouponId())
                .promoType(PromoType.FREE_ROUND)
                .vendorTransactionId(fundInfo.getTransferId())
                .vendorPayoutAmount(fundInfo.getAmount())
                .vendorPlayerUsername(fundTransferRequest.getAccountId())
                .vendorCurrency(fundInfo.getCurrencyCode())
                .token(fundTransferRequest.getToken())
                .vendorGameCode(request.getVendorGameCode())
                .vendorSessionToken(fundTransferRequest.getToken())
                .vendorTransactionTime(VendorService.dateTimeConvert(fundInfo.getDtEvent()))
                .build();
    }
}
