package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class RollbackDataMapper {
    public RollbackData toRollbackData(BetRollbackContext context, BetRollbackConfig config) {
        return new RollbackData() {
            @Override
            public String getRollbackId() {
                if (config.getRollbackType() == null) {
                    throw new InternalServerException("rollbackType is not set");
                }
                return switch (config.getRollbackType()) {
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
                return Optional.ofNullable(context.getRoundId()).orElse(context.getVendorBetId());
            }
        };
    }
}
