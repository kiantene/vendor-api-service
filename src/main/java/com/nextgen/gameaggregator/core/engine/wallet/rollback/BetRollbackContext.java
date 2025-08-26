package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Builder;
import lombok.Data;

/**
 * Field mapping rules for Rollback Request:
 * <p></p>
 * <p>idempotencyKey - Unique key to stop duplicate transactions.</p>
 * <p>vendorBetId    - Same as bet request vendorBetId. Used if rollbackType = BY_BET.</p>
 * <p>roundId        - Same as bet request roundId. Used if rollbackType = BY_ROUND.</p>
 */
@Data
@Builder
public class BetRollbackContext implements GameSessionData {
    private RollbackType rollbackType;
    private String traceId;
    private String idempotencyKey;
    private String vendorBetId;
    private String roundId;
    private String vendorPlayerUsername;
    private String token;
    private String vendorSessionToken;
    private Long timestamp;

    private BaseVendorService vendorService;
    private GameSession gameSession;
    private HttpRequestLog httpRequestLog;

    /**
     * Indicates whether settled bet data must be fetched from the warehouse
     * before performing the rollback operation.
     */
    private boolean retrieveSettledBet;
}
