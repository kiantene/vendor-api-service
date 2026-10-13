package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import lombok.Data;

import java.util.List;

/**
 * Holds the per-request {@link CreditContext} + {@link CreditConfig} pair, plus the debits the
 * credit names: loaded once by the wrapper, read by {@link CreditSessionResolver} and
 * {@link CreditPolicy}.
 */
@Data
public class CreditWrapperContext {
    private CreditContext creditContext;
    private CreditConfig config;
    /** As {@link DebitReferenceGuard#load} returns it: request order, {@code null} for a missing doc. */
    private List<KvDoc<DebitTxn>> referencedDebits = List.of();

    public CreditWrapperContext(CreditContext context) {
        this.creditContext = context;
        this.config = new CreditConfig();
    }
}
