package com.nextgen.gameaggregator.vendor.spribe.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType;
import org.springframework.stereotype.Component;

@Component
public class RollbackContextMapper implements BetRollbackContextMapper<RollbackDto> {
    @Override
    public BetRollbackContext toInternal(RollbackDto vendorRequest) {
        return BetRollbackContext.builder()
                .rollbackType(RollbackType.BY_BET)
                .idempotencyKey(vendorRequest.getRollback_provider_tx_id())
                .vendorBetId(vendorRequest.getRollback_provider_tx_id())
                .roundId(vendorRequest.getProvider_tx_id())
                .vendorPlayerUsername(vendorRequest.getUser_id())
                .vendorSessionToken(vendorRequest.getSession_token())
                .timestamp(vendorRequest.getVendorSettledTime())
                .build();
    }
}
