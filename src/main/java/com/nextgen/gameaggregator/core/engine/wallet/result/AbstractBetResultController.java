package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AbstractBetResultController<Q, R> {
    protected final BetResultContextMapper<Q> requestMapper;
    protected final BetResultVendorResponseMapper<R> responseMapper;
    protected final WalletBetResultServiceWrapper walletBetResultService;

    protected AbstractBetResultController(BetResultContextMapper<Q> requestMapper,
                                          BetResultVendorResponseMapper<R> responseMapper,
                                          WalletBetResultServiceWrapper walletBetResultService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.walletBetResultService = walletBetResultService;
    }

    protected R processRequest(Q request,
                               Consumer<BetResultContext> preProcessHook,
                               BiConsumer<BetResultContext, R> postProcessHook) {

        BetResultContext context = mapToInternal(request);

        if (preProcessHook != null) preProcessHook.accept(context);

        PlayerBalanceData balanceData = walletBetResultService
                .initialise(context)
                .configure(this::configure)
                .process();

        R response = mapToVendor(context, balanceData);

        if (postProcessHook != null) postProcessHook.accept(context, response);

        return response;
    }

    protected final R processRequest(Q request) {
        return processRequest(request, null, null);
    }

    protected R processRequest(Q request, Consumer<BetResultContext> preProcessHook) {
        return processRequest(request, preProcessHook, null);
    }

    protected R processRequest(Q request, BiConsumer<BetResultContext, R> postProcessHook) {
        return processRequest(request, null, postProcessHook);
    }

    protected BetResultContext mapToInternal(Q request) {
        return requestMapper.toInternal(request);
    }

    protected R mapToVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return responseMapper.toVendor(context, balanceData);
    }

    protected void configure(BetResultConfig config) {
        // override for config
    }
}
