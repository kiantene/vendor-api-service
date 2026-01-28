package com.nextgen.gameaggregator.core.context;

import lombok.Builder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Builder
public class VendorExceptionContext {

    private Object[] args;

    /**
     * Return the first present request that matches one of the allowed types
     */
    public Optional<Object> getAnyPresentClass(List<Class<?>> classTypes) {
        return Arrays.stream(args)
                .filter(arg -> arg != null && classTypes.stream().anyMatch(c -> c.isInstance(arg)))
                .findFirst();
    }

    /**
     * Example: original get(Class<T>) method
     */
    public <T> Optional<T> getClass(Class<T> classType) {
        return Arrays.stream(args)
                .filter(classType::isInstance)
                .map(classType::cast)
                .findFirst();
    }
}
