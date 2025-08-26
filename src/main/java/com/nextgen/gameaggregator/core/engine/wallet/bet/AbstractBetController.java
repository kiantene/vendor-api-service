package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AbstractBetController<Q, R> {
    protected final BetContextMapper<Q> requestMapper;
    protected final BetVendorResponseMapper<R> responseMapper;
    protected final WalletBetService walletBetService;

    protected AbstractBetController(BetContextMapper<Q> requestMapper,
                                    BetVendorResponseMapper<R> responseMapper,
                                    WalletBetService walletBetService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.walletBetService = walletBetService;
    }

    protected R processRequest(Q request,
                               Consumer<BetContext> preProcessHook,
                               BiConsumer<BetContext, R> postProcessHook) {

        BetContext context = mapToInternal(request);

        if (preProcessHook != null) preProcessHook.accept(context);

        PlayerBalanceData balanceData = walletBetService
                .process(context);

        R response = mapToVendor(context, balanceData);

        if (postProcessHook != null) postProcessHook.accept(context, response);

        return response;
    }

    protected final R processRequest(Q request) {
        return processRequest(request, null, null);
    }

    protected R processRequest(Q request, Consumer<BetContext> preProcessHook) {
        return processRequest(request, preProcessHook, null);
    }

    protected R processRequest(Q request, BiConsumer<BetContext, R> postProcessHook) {
        return processRequest(request, null, postProcessHook);
    }

    protected BetContext mapToInternal(Q request) {
        return requestMapper.toInternal(request);
    }

    protected R mapToVendor(BetContext context, PlayerBalanceData balanceData) {
        return responseMapper.toVendor(context, balanceData);
    }
}
