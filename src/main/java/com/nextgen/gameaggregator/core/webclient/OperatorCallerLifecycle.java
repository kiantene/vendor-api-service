package com.nextgen.gameaggregator.core.webclient;

public interface OperatorCallerLifecycle {
    default void onBeforeSend(ClientApiRequest<?> request) {}
    default void onResponse(ClientApiRequest<?> request, ClientApiResult result) {}
    default void onError(ClientApiRequest<?> request, Throwable error) {}
    default void onComplete(ClientApiRequest<?> request, ClientApiResult result) {}

    static OperatorCallerLifecycle noop() {
        return new OperatorCallerLifecycle() {};
    }
}
