package com.nextgen.gameaggregator.vendor.evoplay.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import org.springframework.stereotype.Component;

@Component
class RollbackRequestMapper implements BetRollbackContextMapper<CallbackDto> {
    @Override
    public BetRollbackContext toInternal(CallbackDto request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getCallback_id())
                .token(request.getToken())
                .vendorGameCode(request.getData().getDetailsDto().getGame().getGame_id())
                .roundId(request.getData().getRound_id())
                .vendorBetId(request.getData().getRefund_action_id())
                .vendorPlayerUsername(request.getUsername())
                .build();
    }

}
