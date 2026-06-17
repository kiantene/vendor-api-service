package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Base controller for handling vendor requests with a standardized lifecycle.
 *
 * @param <Q> The Vendor Request type (the incoming payload).
 * @param <R> The Vendor Response type (the outgoing payload).
 * @param <C> The Internal Context type (must extend VendorRequestContext).
 * @param <T> The Internal Service Data type (the result returned by the core logic).
 */
public abstract class AbstractFrameworkController<Q, R, C extends VendorRequestContext, T> {

    protected AbstractFrameworkController() {}

    protected R processRequest(Q request,
                               Consumer<C> preProcessHook,
                               BiConsumer<C, R> postProcessHook) {

        C context = mapToInternal(request);
        context.setVendorClassName(LogContextHolder.getVendorClassName());

        if (preProcessHook != null) preProcessHook.accept(context);

        T data = executeService(context, request);

        R response = mapToVendor(request, context, data);

        if (postProcessHook != null) postProcessHook.accept(context, response);

        return response;
    }

    protected abstract T executeService(C context, Q request);

    protected abstract C mapToInternal(Q request);

    protected abstract R mapToVendor(Q request, C context, T data);

    protected final R processRequest(Q request) { return processRequest(request, null, null); }
}