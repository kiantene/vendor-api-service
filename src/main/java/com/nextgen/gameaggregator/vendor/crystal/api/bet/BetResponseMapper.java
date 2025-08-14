package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
class BetResponseMapper implements VendorResponseMapper<BetContext, BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return BetResponse.builder()
                .dataVo(BetResponse.DataVo.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .actionId(context.getExternalTransactionId())
                        .build())
                .errorVo(null)
                .build();
    }
}
