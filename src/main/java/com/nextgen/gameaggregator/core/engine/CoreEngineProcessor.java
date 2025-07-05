package com.nextgen.gameaggregator.core.engine;

public interface CoreEngineProcessor<C, R> {
    void process(C context);
    void onSuccess(C context, R result);
    void onError(C context, Exception ex);
}
