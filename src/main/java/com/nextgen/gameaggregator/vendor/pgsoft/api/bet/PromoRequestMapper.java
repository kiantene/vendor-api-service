package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import org.springframework.stereotype.Component;

@Component("pgsoftPromoPayoutRequestMapper")
public class PromoRequestMapper implements VendorRequestMapper<PromoPayoutContext, CashTransferInOutDto> {
    @Override
    public PromoPayoutContext toInternal(CashTransferInOutDto vendorRequest) {
        return PromoPayoutContext.builder()
                .traceId(UuidUtil.newUuidV7StringRaw())
                .vendorClassName(Endpoints.CLASS_NAME)
                .idempotencyKey(vendorRequest.getFreeGameTransactionId())
                .vendorPlayerUsername(vendorRequest.getPlayerName())
                .vendorCurrency(vendorRequest.getCurrencyCode())
                .vendorGameCode(vendorRequest.getGameId())
                // promo payout history
                .vendorTransactionId(vendorRequest.getFreeGameTransactionId())
                .payoutAmount(vendorRequest.getReal_transfer_amount())
                .vendorTransactionTime(vendorRequest.getCreateTime())
                .build();
    }
}
