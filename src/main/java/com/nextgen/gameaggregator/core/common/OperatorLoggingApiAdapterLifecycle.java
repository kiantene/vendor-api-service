package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.api.ApiAdapterLifeCycle;
import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;

public class OperatorLoggingApiAdapterLifecycle implements ApiAdapterLifeCycle<OperatorApiRequest, ApiResult> {
    private final LogContext logContext;

    public OperatorLoggingApiAdapterLifecycle(LogContext logContext) {
        this.logContext = logContext;
    }

    public static OperatorLoggingApiAdapterLifecycle get() {
        return new OperatorLoggingApiAdapterLifecycle(LogContextHolder.get());
    }

    @Override
    public void onBeforeSend(OperatorApiRequest request) {
        logContext.setAgentId(request.getAgentId());
        logContext.setApiStart(request.getStart().toEpochMilli());
        logContext.setApiUrl(request.getUrl());
        logContext.setApiBody(request.getBodyAsJson());
    }

    @Override
    public void onResponse(OperatorApiRequest request, ApiResult result) {
        logContext.setApiResponse(result.getRawResponse());
        logContext.setApiEnd(result.getEnd().toEpochMilli());
    }

    @Override
    public void onError(OperatorApiRequest request, ApiResult result, Throwable error) {
        logContext.setApiEnd(result.getEnd().toEpochMilli());
        logContext.setException(error);
    }
}
