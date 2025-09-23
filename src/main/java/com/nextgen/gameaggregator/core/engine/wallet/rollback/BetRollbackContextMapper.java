package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

/**
 * Mapper class that handles bet rollback requests.
 * <p>
 * Field mapping rules are defined in {@link BetRollbackContext}.
 *
 * @see BetRollbackContext
 */
@FunctionalInterface
public interface BetRollbackContextMapper<V> extends VendorRequestMapper<BetRollbackContext, V> {
    BetRollbackContext toInternal(V request);
}
