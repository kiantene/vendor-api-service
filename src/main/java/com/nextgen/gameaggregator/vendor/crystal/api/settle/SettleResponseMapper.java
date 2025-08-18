package com.nextgen.gameaggregator.vendor.crystal.api.settle;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
class SettleResponseMapper implements VendorResponseMapper<BetResultContext, SettleResponse> {
    @Override
    public SettleResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return SettleResponse.builder()
                .data(SettleResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .actionId(context.getVendorBetId())
                        .build())
                .build();
    }
}