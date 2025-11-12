package com.nextgen.gameaggregator.core.webclient;

import com.nextgen.core.api.ApiResult;
import com.nextgen.core.api.ReactiveApiAdapter;
import com.nextgen.gameaggregator.core.common.OperatorLoggingApiAdapterLifecycle;
import com.nextgen.gameaggregator.core.logging.LogContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OperatorReactiveApiAdapter extends ReactiveApiAdapter<OperatorApiRequest, ApiResult> {

    public OperatorReactiveApiAdapter() {
        super("operator-api-pool");
    }

    @Override
    protected ApiResult onApiSuccess(ApiResult apiResult) {
        return apiResult;
    }

    @Override
    protected ApiResult onApiError(ApiResult apiResult) {
        return apiResult;
    }

    @Override
    protected ApiResult onApiComplete(ApiResult apiResult) {
        return apiResult;
    }

    public Mono<ApiResult> execute(OperatorApiRequest apiRequest, LogContext logContext) {
        return execute(apiRequest, new OperatorLoggingApiAdapterLifecycle(logContext));
    }
}
