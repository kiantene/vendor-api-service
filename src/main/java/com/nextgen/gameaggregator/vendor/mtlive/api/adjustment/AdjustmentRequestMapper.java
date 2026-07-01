package com.nextgen.gameaggregator.vendor.mtlive.api.adjustment;

import com.nextgen.gameaggregator.core.engine.wallet.adjustment.AdjustmentContext;
import com.nextgen.gameaggregator.core.engine.wallet.adjustment.AdjustmentContextMapper;
import org.springframework.stereotype.Component;

@Component
public class AdjustmentRequestMapper implements AdjustmentContextMapper<AdjustmentRequest> {
    @Override
    public AdjustmentContext toInternal(AdjustmentRequest request) {
        return AdjustmentContext.builder()
                .idempotencyKey(request.getBet_sn())
                .vendorBetId(request.getBet_sn())
                .roundId(request.getBet_sn())
                .vendorPlayerUsername(request.getUser_id())
                .winAmount(request.getWin_money())
                .build();
    }
}
