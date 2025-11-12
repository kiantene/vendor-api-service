package com.nextgen.gameaggregator.vendor.facai.api.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.PayoutTransaction;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
class PayoutTransactionMapper {
    private PayoutTransactionMapper() {
    }

    public static List<PayoutTransaction> map(String currency, PromoPayoutList listRequest) {
        List<PromoPayout> payouts = listRequest.getList();

        if (payouts.isEmpty()) return Collections.emptyList();

        return payouts.stream()
                .filter(Objects::nonNull)
                .map(p -> toPayoutTxn(p, currency, listRequest.getTimestamp()))
                .toList();
    }

    private static PayoutTransaction toPayoutTxn(PromoPayout payout, String currency, Long vendorTransactionTime) {
        return PayoutTransaction.builder()
                .vendorTransactionId(payout.getTrsID())
                .vendorPlayerUsername(payout.getMemberAccount())
                .vendorCurrency(currency)
                .vendorCampaignCode(payout.getEventID())
                .vendorTransactionTime(vendorTransactionTime)
                .vendorPayoutAmount(payout.getPoints())
                .build();

    }
}
