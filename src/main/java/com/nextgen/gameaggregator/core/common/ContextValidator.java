package com.nextgen.gameaggregator.core.common;

public interface ContextValidator<T> {
    void validateOrThrow(T context);
}
