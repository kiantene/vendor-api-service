package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

/**
 * Mapper class that handles bet requests.
 * <p>
 * Field mapping rules are defined in {@link BetContext}.
 *
 * @see BetContext
 */
public interface BetContextMapper<V> extends VendorRequestMapper<BetContext, V> {
    @Override
    BetContext toInternal(V request);
}
