package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.BaseVendorService;
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

    private BaseVendorService vendorService;
    private GameSession gameSession;
    private HttpRequestLog httpRequestLog;
}
