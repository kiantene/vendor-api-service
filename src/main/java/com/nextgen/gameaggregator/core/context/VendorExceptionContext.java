package com.nextgen.gameaggregator.core.context;

import lombok.Builder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Builder
public class VendorExceptionContext {
    private Object[] args;
    private Map<String, String> requestHeaders;

    public static VendorExceptionContext of(Object[] args, Map<String, String> requestHeaders) {
        return VendorExceptionContext.builder()
                .args(args)
                .requestHeaders(requestHeaders)
                .build();
    }

    /**
     * Retrieves the first object from the `args` array that is an instance of any class
     * in the provided list of class types.
     */
    public Optional<Object> getAnyPresentClass(List<Class<?>> classTypes) {
        return Arrays.stream(args)
                .filter(arg -> arg != null && classTypes.stream().anyMatch(c -> c.isInstance(arg)))
                .findFirst();
    }

    /**
     * Retrieves the first object from the list of arguments (`args`) that is an instance
     * of the specified class type.
     */
    public <T> Optional<T> getClass(Class<T> classType) {
        return Arrays.stream(args)
                .filter(classType::isInstance)
                .map(classType::cast)
                .findFirst();
    }

    /**
     * Retrieves the value of a specific header from the request headers.
     */
    public Optional<String> getHeader(String key) {
        return Optional
            .ofNullable(requestHeaders)
            .map(headers -> headers.get(key));
    }
}
