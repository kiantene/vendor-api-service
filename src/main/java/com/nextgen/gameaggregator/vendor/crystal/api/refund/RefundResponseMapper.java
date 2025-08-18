package com.nextgen.gameaggregator.vendor.crystal.api.refund;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class RefundResponseMapper implements VendorResponseMapper<BetRollbackContext, RefundResponse> {

    @Override
    public RefundResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return RefundResponse.builder()
                .data(RefundResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .actionId(context.getVendorBetId())
                        .build())
                .build();
    }
}