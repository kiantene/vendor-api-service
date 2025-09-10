package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AbstractProcessorController<Q, R, C> {
    protected final VendorRequestMapper<C, Q> requestMapper;
    protected final VendorResponseMapper<C, R> responseMapper;

    protected AbstractProcessorController(VendorRequestMapper<C, Q> requestMapper,
                                          VendorResponseMapper<C, R> responseMapper) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
    }

    protected R processRequest(Q request,
                               Consumer<C> preProcessHook,
                               BiConsumer<C, R> postProcessHook) {

        C context = mapToInternal(request);

        if (preProcessHook != null) preProcessHook.accept(context);

        PlayerBalanceData data = executeService(context, request);

        R response = mapToVendor(context, data);

        if (postProcessHook != null) postProcessHook.accept(context, response);

        return response;
    }

    protected final R processRequest(Q request) {
        return processRequest(request, null, null);
    }

    protected R processRequest(Q request, Consumer<C> preProcessHook) {
        return processRequest(request, preProcessHook, null);
    }

    protected R processRequest(Q request, BiConsumer<C, R> postProcessHook) {
        return processRequest(request, null, postProcessHook);
    }

    protected C mapToInternal(Q request) {
        return requestMapper.toInternal(request);
    }

    protected R mapToVendor(C context, PlayerBalanceData balanceData) {
        return responseMapper.toVendor(context, balanceData);
    }

    protected abstract PlayerBalanceData executeService(C context, Q request);
}
