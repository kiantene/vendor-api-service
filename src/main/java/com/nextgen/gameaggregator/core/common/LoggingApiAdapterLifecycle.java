package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.api.ApiAdapterLifeCycle;
import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;

public class LoggingApiAdapterLifecycle implements ApiAdapterLifeCycle<ApiRequest, ApiResult> {
    private final LogContext logContext;

    public LoggingApiAdapterLifecycle(LogContext logContext) {
        this.logContext = logContext;
    }

    public static LoggingApiAdapterLifecycle get() {
        return new LoggingApiAdapterLifecycle(LogContextHolder.get());
    }

    @Override
    public void onBeforeSend(ApiRequest request) {
        logContext.setApiStart(request.getStart().toEpochMilli());
        logContext.setApiUrl(request.getUrl());
        logContext.setApiBody(request.getBodyAsJson());
    }

    @Override
    public void onResponse(ApiRequest request, ApiResult result) {
        logContext.setApiResponse(result.getRawResponse());
        logContext.setApiEnd(result.getEnd().toEpochMilli());
    }

    @Override
    public void onError(ApiRequest request, ApiResult result, Throwable error) {
        logContext.setApiEnd(result.getEnd().toEpochMilli());
        logContext.setException(error);
    }
}
