package com.nextgen.gameaggregator.vendor.cockfight6.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<CommonRequest> {

    @Override
    public BetRollbackContext toInternal(CommonRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(String.valueOf(request.getRecordId()))
                .vendorPlayerUsername(request.getPlayerName())
                .vendorBetId(String.valueOf(request.getRecordId()))
                .build();
    }
}
