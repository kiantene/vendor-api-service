package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

/**
 * Per-vendor mapper: vendor wire request {@code V} → {@link DebitContext}.
 * <p>
 * Field mapping rules are defined in {@link DebitContext}.
 *
 * @see DebitContext
 */
public interface DebitContextMapper<V> extends VendorRequestMapper<DebitContext, V> {
    @Override
    DebitContext toInternal(V request);
}
