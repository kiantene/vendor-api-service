package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

/**
 * Sibling of {@link com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController}
 * for vendors that speak debit/credit on the wire. Handles BOTH settlement and rollback
 * via the {@link CreditContext#isRefund()} discriminator set by the per-vendor mapper.
 *
 * @param <Q> vendor request type
 * @param <R> vendor response type
 */
public abstract class AbstractCreditController<Q, R> extends AbstractProcessorController<Q, R, CreditContext> {

    protected final WalletCreditService walletService;

    protected AbstractCreditController(CreditContextMapper<Q> requestMapper,
                                       CreditVendorResponseMapper<R> responseMapper,
                                       WalletCreditService walletService) {
        super(requestMapper, responseMapper);
        this.walletService = walletService;
    }

    @Override
    protected PlayerBalanceData executeService(CreditContext context, Q request) {
        return walletService
                .initialise(context)
                .configure(config -> configure(config, request))
                .process();
    }

    /** Override to customize {@link CreditConfig} per request. Default no-op. */
    protected void configure(CreditConfig config, Q request) {
        // no-op
    }
}
