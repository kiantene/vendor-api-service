package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

/**
 * Per-vendor mapper: vendor wire request {@code V} → {@link CreditContext}.
 * <p>
 * The mapper is responsible for setting {@link CreditContext#isRefund()} from the
 * vendor wire (settle → false, refund/rollback → true).
 */
public interface CreditContextMapper<V> extends VendorRequestMapper<CreditContext, V> {
    @Override
    CreditContext toInternal(V request);
}
