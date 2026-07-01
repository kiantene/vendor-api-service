package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

/**
 * Mapper class that handles bet rollback requests.
 * <p>
 * Field mapping rules are defined in {@link AdjustmentContext}.
 *
 * @see AdjustmentContext
 */
@FunctionalInterface
public interface AdjustmentContextMapper<V> extends VendorRequestMapper<AdjustmentContext, V> {
    AdjustmentContext toInternal(V request);
}
