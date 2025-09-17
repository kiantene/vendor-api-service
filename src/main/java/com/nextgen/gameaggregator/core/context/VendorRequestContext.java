package com.nextgen.gameaggregator.core.context;

import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public abstract class VendorRequestContext implements GameSessionData {
    /**
     * A unique identifier for tracing requests across distributed services.
     * Used for debugging and logging to follow the lifecycle of a bet transaction.
     */
    protected String traceId;
    /**
     * Provided by the vendor to ensure the same bet request is not processed multiple times.
     */
    protected String idempotencyKey;

    protected String vendorClassName;
    protected String vendorPlayerUsername;
    protected String vendorCurrency;
    protected String vendorGameCode;
    protected String vendorSessionToken;  // Vendor's game session token provided in vendor's request.

    /**
     * GA generated game session token during game launch.
     * Need to map if vendor returns back GA's token.
     */
    protected String token;

    public String getTransactionId() {
        return idempotencyKey;
    }
}
