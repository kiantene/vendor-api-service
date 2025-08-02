package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BetRollbackContext {
    private RollbackType rollbackType;
    private String traceId;
    private String idempotencyKey;
    private String betId;
    private String roundId;
    private String vendorPlayerUsername;
    private String vendorSessionToken;
    private Long timestamp;
}
