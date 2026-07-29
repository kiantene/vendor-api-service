package com.nextgen.gameaggregator.vendor.spribe.api.v2.result;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class FreebetPayoutContextMapper implements PromoPayoutContextMapper<BetResultRequest> {
    @Override
    public PromoPayoutContext toInternal(BetResultRequest request) {
        return PromoPayoutContext.builder()
                .idempotencyKey(request.getProviderTxId())
                .vendorCampaignCode(request.getOperatorFreeBetId())
                .promoType(PromoType.FREE_ROUND)
                .vendorTransactionId(request.getProviderTxId())
                .vendorPayoutAmount(AmountConverter.convertUnitToBalance(request.getAmount()))
                .vendorPlayerUsername(request.getUserId())
                .vendorCurrency(request.getCurrency())
                .vendorGameCode(request.getGame())
                .vendorSessionToken(request.getSessionToken())
                .build();
    }
}
