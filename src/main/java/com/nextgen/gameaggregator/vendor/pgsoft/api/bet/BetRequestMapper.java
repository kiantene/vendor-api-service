package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.engine.promo.PromoPayoutContext;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import org.springframework.stereotype.Component;

@Component("pgsoftBetRequestMapper")
public class BetRequestMapper implements VendorRequestMapper<CashTransferInOutDto, PromoPayoutContext> {
    @Override
    public PromoPayoutContext toInternal(CashTransferInOutDto vendorRequest) {
        PromoPayoutContext context = new PromoPayoutContext();
        context.setIdempotencyKey(vendorRequest.getBetId());
        context.setVendorPlayerUsername(vendorRequest.getPlayerName());
        context.setVendorCurrency(vendorRequest.getCurrencyCode());
        context.setAmount(vendorRequest.getWinAmount());
        context.setTimestamp(vendorRequest.getCreateTime());

        return context;
    }
}
