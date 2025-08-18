package com.nextgen.gameaggregator.core.engine.wallet.balance;


import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AbstractBalanceController<Q, R> {
    protected final BalanceContextMapper<Q> requestMapper;
    protected final BalanceVendorResponseMapper<R> responseMapper;
    protected final WalletBalanceService walletBalanceService;

    protected AbstractBalanceController(BalanceContextMapper<Q> requestMapper,
                                        BalanceVendorResponseMapper<R> responseMapper,
                                        WalletBalanceService walletBalanceService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.walletBalanceService = walletBalanceService;
    }

    protected R processRequest(Q request,
                               Consumer<BalanceContext> preProcessHook,
                               BiConsumer<BalanceContext, R> postProcessHook) {

        BalanceContext context = mapToInternal(request);

        if (preProcessHook != null) preProcessHook.accept(context);

        PlayerBalanceData balanceData = walletBalanceService
                .process(context);

        R response = mapToVendor(context, balanceData);

        if (postProcessHook != null) postProcessHook.accept(context, response);

        return response;
    }

    protected final R processRequest(Q request) {
        return processRequest(request, null, null);
    }

    protected R processRequest(Q request, Consumer<BalanceContext> preProcessHook) {
        return processRequest(request, preProcessHook, null);
    }

    protected R processRequest(Q request, BiConsumer<BalanceContext, R> postProcessHook) {
        return processRequest(request, null, postProcessHook);
    }

    protected BalanceContext mapToInternal(Q request) {
        return requestMapper.toInternal(request);
    }

    protected R mapToVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return responseMapper.toVendor(context, balanceData);
    }
}
