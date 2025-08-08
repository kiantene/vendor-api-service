package com.nextgen.gameaggregator.core.common;

public interface ContextEnricher<T> {
    void enrich(T context);
}
