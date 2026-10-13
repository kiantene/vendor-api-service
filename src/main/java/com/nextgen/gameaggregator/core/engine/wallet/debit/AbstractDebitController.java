package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

/**
 * Sibling of {@link com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController}
 * for vendors that speak debit/credit on the wire.
 * <p>
 * Subclasses provide a per-vendor request mapper ({@link DebitContextMapper}), a per-vendor
 * response mapper ({@link DebitVendorResponseMapper}), and the shared
 * {@link WalletDebitService} bean. They invoke {@link #processRequest(Object)} from
 * their inbound entry point.
 *
 * @param <Q> vendor request type
 * @param <R> vendor response type
 */
public abstract class AbstractDebitController<Q, R> extends AbstractProcessorController<Q, R, DebitContext> {

    protected final WalletDebitService walletService;

    protected AbstractDebitController(DebitContextMapper<Q> requestMapper,
                                      DebitVendorResponseMapper<R> responseMapper,
                                      WalletDebitService walletService) {
        super(requestMapper, responseMapper);
        this.walletService = walletService;
    }

    @Override
    protected PlayerBalanceData executeService(DebitContext context, Q request) {
        return walletService
                .initialise(context)
                .configure(config -> configure(config, request))
                .process();
    }

    /** Override to customize {@link DebitConfig} per request. Default no-op. */
    protected void configure(DebitConfig config, Q request) {
        // no-op
    }
}
