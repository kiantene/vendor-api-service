package com.nextgen.gameaggregator.vendor.lucky365.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.lucky365.util.LoginIds;
import com.nextgen.gameaggregator.vendor.lucky365.util.TimeStamp;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {

        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getId())
                .vendorBetId(vendorRequest.getOrderCode())
                .roundId(vendorRequest.getOrderCode())
                .vendorPlayerUsername(LoginIds.forLookup(vendorRequest.getLoginId()))
                .timestamp(TimeStamp.convertTimeStamp(vendorRequest.getActionDate()))
                .build();
    }
}