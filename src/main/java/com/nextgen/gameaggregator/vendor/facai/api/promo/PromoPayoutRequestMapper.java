package com.nextgen.gameaggregator.vendor.facai.api.promo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.promo.payout.PayoutTransaction;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromoPayoutRequestMapper implements PromoPayoutContextMapper<PromoPayoutRequest> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public PromoPayoutContext toInternal(PromoPayoutRequest vendorRequest) {
        PromoPayoutContext context = PromoPayoutContext.builder()
                .vendorCurrency(vendorRequest.getCurrency())
                .promoType(PromoType.FREE_ROUND)
                .build();

        PromoPayoutList list = deserialize(vendorRequest.getParamsJsonString());
        var payoutTransactions = PayoutTransactionMapper.map(vendorRequest.getCurrency(), list);
        context.setPayoutTransactions(payoutTransactions);
        context.setVendorTransactionTime(list.getTimestamp());

        if (payoutTransactions.size() != 1) {
            if (payoutTransactions.isEmpty()) {
                return context;
            }
            throw new InvalidRequestException("expected exactly 1 payout transaction, but got " + payoutTransactions.size());
        }

        PayoutTransaction tx = payoutTransactions.get(0);
        context.setVendorCampaignCode(tx.getVendorCampaignCode());
        context.setIdempotencyKey(tx.getVendorTransactionId());

        return context;
    }

    private PromoPayoutList deserialize(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, PromoPayoutList.class);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("cannot deserialize list: " + json);
        }
    }

}
