package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdjustmentDataMapper {
    public AdjustmentData toAdjustmentData(AdjustmentContext context) {
        return new AdjustmentData() {
            @Override
            public String getExternalTransactionId() {
                return context.getIdempotencyKey();
            }

            @Override
            public String getVendorBetId() {
                return context.getVendorBetId();
            }

            @Override
            public String getRoundId() {
                return context.getRoundId();
            }

            @Override
            public String getGameId() {
                return context.getVendorGameCode();
            }

            @Override
            public BigDecimal getAdjustmentAmount() {
                return context.getAdjustmentAmount();
            }

            @Override
            public Long getTimestamp() {
                return context.getTimestamp();
            }

        };
    }
}
