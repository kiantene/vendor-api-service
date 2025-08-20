package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
                return Optional.ofNullable(context.getTimestamp()).orElse(System.currentTimeMillis());
            }

            @Override
            public String getRoundId() {
                return Optional.ofNullable(context.getRoundId()).orElse(context.getVendorBetId());
            }
        };
    }
}
