package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

/**
 * Mapper class that handles bet result requests.
 * <p>
 * Field mapping rules are defined in {@link BetResultContext}.
 *
 * @see BetResultContext
 */
public interface BetResultContextMapper<V> extends VendorRequestMapper<BetResultContext, V> {
    @Override
    BetResultContext toInternal(V request);
}
