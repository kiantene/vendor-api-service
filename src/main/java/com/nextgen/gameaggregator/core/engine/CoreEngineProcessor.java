package com.nextgen.gameaggregator.core.engine;

import com.nextgen.gameaggregator.core.common.ClientApiRequest;

public interface CoreEngineProcessor<C, R> {
    PlayerBalanceData process(C context);
    void onSuccess(C context, R result);
    void onError(C context, ClientApiRequest<?> clientApiRequest, Exception ex);
}
