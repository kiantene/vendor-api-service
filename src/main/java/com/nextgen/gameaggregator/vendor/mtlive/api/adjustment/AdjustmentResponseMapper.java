package com.nextgen.gameaggregator.vendor.mtlive.api.adjustment;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.adjustment.AdjustmentContext;
import com.nextgen.gameaggregator.core.engine.wallet.adjustment.AdjustmentVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class AdjustmentResponseMapper implements AdjustmentVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(AdjustmentContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .timestamp(context.getTimestamp())
                .data(SuccessResponse.Data.builder()
                        .bet_sn(context.getVendorBetId())
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
