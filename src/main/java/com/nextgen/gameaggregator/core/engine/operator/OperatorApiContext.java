package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import lombok.Data;

/**
 * Context object for executing an operator API call.
 *
 * <p>
 * This class wraps all the necessary metadata for interacting with an operator:
 * - context: vendor-specific request context
 * - round: the current GameRound
 * - transaction: the GameTransaction associated with this operation
 * - timeoutInMillis: vendor-specific per-call timeout (defaults to vendor default, 4000ms)
 * </p>
 *
 * <p>
 * Key points:
 * - Generic type <C extends VendorRequestContext> allows flexibility for different vendor implementations.
 * - The class is immutable except for the timeoutInMillis field, which can be adjusted if needed.
 * - Provides convenience factory method `of(...)` to simplify construction without exposing the private constructor.
 * </p>
 *
 * <p>
 * Usage:
 * - Passed to adapters (OperatorRequestAdapter) or services (OperatorApiService) to provide unified access to
 *   request, round, transaction, and timeout information.
 * - Helps remove the need to pass multiple parameters individually to adapters or service methods.
 * </p>
 */
@Data
public class OperatorApiContext<C extends VendorRequestContext> {
    private final C context;
    private final GameRound round;
    private final GameTransaction transaction;

    /**
     * Optional timeout for this operator API call (milliseconds).
     * Defaults to the vendor default timeout (4000ms).
     */
    private int timeoutInMillis = AbstractVendorConfig.DEFAULT_TIMEOUT;

    private OperatorApiContext(C context, GameRound round, GameTransaction transaction) {
        this.context = context;
        this.round = round;
        this.transaction = transaction;
    }

    public static <C extends VendorRequestContext> OperatorApiContext<C> of(
            C context,
            GameRound round,
            GameTransaction transaction) {
        return new OperatorApiContext<>(context, round, transaction);
    }
}
