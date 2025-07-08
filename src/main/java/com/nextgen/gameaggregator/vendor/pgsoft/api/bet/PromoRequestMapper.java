package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.core.util.UuidUtil;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import org.springframework.stereotype.Component;

@Component("pgsoftPromoPayoutRequestMapper")
public class PromoRequestMapper implements VendorRequestMapper<PromoPayoutContext, CashTransferInOutDto> {
    @Override
    public PromoPayoutContext toInternal(CashTransferInOutDto vendorRequest) {
        return PromoPayoutContext.builder()
                .traceId(UuidUtil.newUuidV7StringRaw())
                .vendorClassName(Endpoints.CLASS_NAME)
                .idempotencyKey(vendorRequest.getBetId())
                .vendorPlayerUsername(vendorRequest.getPlayerName())
                .vendorCurrency(vendorRequest.getCurrencyCode())
                .amount(vendorRequest.getWinAmount())
                .timestamp(vendorRequest.getCreateTime())
                // promo payout history
                .betAmount(vendorRequest.getBetAmount())
                .winAmount(vendorRequest.getWinAmount())
                .winLoss(vendorRequest.getWinLoss())
                .effectiveTurnover(vendorRequest.getEffectiveTurnover())
                .vendorBetTime(vendorRequest.getVendorBetTime())
                .vendorSettleTime(vendorRequest.getVendorSettleTime())
                .resultTime(vendorRequest.getResultTime())
                .build();
    }
}
