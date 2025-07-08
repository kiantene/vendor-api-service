package com.nextgen.gameaggregator.core.engine;

import com.nextgen.gameaggregator.core.common.ClientRequestAuth;

public interface CoreEngineProcessor<C, R> {
    void process(C context);
    void onSuccess(C context, R result);
    void onError(C context, ClientRequestAuth<?> clientRequestAuth, Exception ex);
}
