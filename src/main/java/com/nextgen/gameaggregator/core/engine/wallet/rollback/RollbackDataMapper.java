package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import org.springframework.stereotype.Component;

@Component
class RollbackDataMapper {
    public RollbackData toRollbackData(BetRollbackContext context) {
        return new RollbackData() {
            @Override
            public String getRollbackId() {
                return switch (context.getRollbackType()) {
                    case BY_BET -> context.getVendorBetId();
                    case BY_ROUND -> context.getRoundId();
                };
            }

            @Override
            public Long getVendorSettledTime() {
                return context.getTimestamp();
            }

            @Override
            public String getRoundId() {
                return context.getRoundId();
            }
        };
    }
}
