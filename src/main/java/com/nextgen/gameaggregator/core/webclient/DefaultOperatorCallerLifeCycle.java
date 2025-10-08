package com.nextgen.gameaggregator.core.webclient;

import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;

public class DefaultOperatorCallerLifeCycle implements OperatorCallerLifecycle {
    private final LogContext logContext;

    public DefaultOperatorCallerLifeCycle(LogContext logContext) {
        this.logContext = logContext;
    }

    public static DefaultOperatorCallerLifeCycle get() {
        return new DefaultOperatorCallerLifeCycle(LogContextHolder.get());
    }

    @Override
    public void onBeforeSend(ClientApiRequest<?> request) {
        logContext.setApiStart(System.currentTimeMillis());
        logContext.setApiUrl(request.getFullUrl());
        logContext.setApiBody(request.getRequestObject());
    }

    @Override
    public void onResponse(ClientApiRequest<?> request, ClientApiResult result) {
        logContext.setApiResponse(result.getRawResponse());
        logContext.setApiEnd(System.currentTimeMillis());
    }

    @Override
    public void onError(ClientApiRequest<?> request, Throwable error) {
        logContext.setApiEnd(System.currentTimeMillis());
        logContext.setException(error);
    }
}
