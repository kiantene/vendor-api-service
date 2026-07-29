package com.nextgen.gameaggregator.vendor.cockfight6.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<CommonRequest> {

    @Override
    public BetRollbackContext toInternal(CommonRequest request) {

        //TO handle different types of requests (1. settle, 2. cancel)
        BetRollbackContext.BetRollbackContextBuilder builder = BetRollbackContext.builder()
                .idempotencyKey(String.valueOf(request.getRecordId()))
                .vendorPlayerUsername(request.getPlayerName())
                .vendorBetId(String.valueOf(request.getRecordId()));

        if (request.getSettle() != null) {
            builder.roundId(String.valueOf(request.getSettle().getGameRoundId()))
                    .timestamp(request.getCreateTime() * 1000);
        }

        return builder.build();
    }
}
