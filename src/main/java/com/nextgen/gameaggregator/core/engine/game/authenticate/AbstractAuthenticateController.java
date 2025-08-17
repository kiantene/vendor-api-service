package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AbstractAuthenticateController<Q, R> {
    protected final AuthenticateContextMapper<Q> requestMapper;
    protected final AuthenticateVendorResponseMapper<R> responseMapper;
    protected final AuthenticateService authenticateService;

    protected AbstractAuthenticateController(AuthenticateContextMapper<Q> requestMapper,
                                             AuthenticateVendorResponseMapper<R> responseMapper,
                                             AuthenticateService authenticateService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.authenticateService = authenticateService;
    }

    protected R processRequest(Q request,
                               Consumer<AuthenticateContext> preProcessHook,
                               BiConsumer<AuthenticateContext, R> postProcessHook) {

        AuthenticateContext context = mapToInternal(request);

        if (preProcessHook != null) preProcessHook.accept(context);

        PlayerBalanceData balanceData = authenticateService
                .process(context);

        R response = mapToVendor(context, balanceData);

        if (postProcessHook != null) postProcessHook.accept(context, response);

        return response;
    }

    protected final R processRequest(Q request) {
        return processRequest(request, null, null);
    }

    protected R processRequest(Q request, Consumer<AuthenticateContext> preProcessHook) {
        return processRequest(request, preProcessHook, null);
    }

    protected R processRequest(Q request, BiConsumer<AuthenticateContext, R> postProcessHook) {
        return processRequest(request, null, postProcessHook);
    }

    protected AuthenticateContext mapToInternal(Q request) {
        return requestMapper.toInternal(request);
    }

    protected R mapToVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return responseMapper.toVendor(context, balanceData);
    }
}
