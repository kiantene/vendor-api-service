package com.nextgen.gameaggregator.vendor.groove.api.freeround;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.groove.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import org.springframework.stereotype.Component;

@Component
public class FreeRoundPayoutRequestMapper implements PromoPayoutContextMapper<BetResultRequest> {
    @Override
    public PromoPayoutContext toInternal(BetResultRequest request) {
        return PromoPayoutContext.builder()
                .idempotencyKey(request.getTransactionid())
                .vendorTransactionId(request.getTransactionid())
                .token(VendorUtil.extractTokenFromSessionId(request.getGamesessionid()))
                .vendorFreeRoundBonusId(request.getFrbid())
                .vendorPlayerUsername(request.getAccountid())
                .vendorPayoutAmount(request.getResult())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
