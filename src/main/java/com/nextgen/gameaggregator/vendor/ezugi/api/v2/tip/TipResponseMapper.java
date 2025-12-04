package com.nextgen.gameaggregator.vendor.ezugi.api.v2.tip;

import java.math.BigInteger;

import org.springframework.stereotype.Component;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet.BetResponse;

@Component
public class TipResponseMapper implements BetResultVendorResponseMapper<BetResponse> {
    @Override
    public BetResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetResponse.builder()
                .token(context.getToken())
                .transactionId(context.getIdempotencyKey())
                .uid(balanceData.getUsername())
                .currency(balanceData.getCurrency())
                .balance(balanceData.getBalance())
                .timestamp(balanceData.getTimestamp())
                .build();
    }
}
