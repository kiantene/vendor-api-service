package com.nextgen.gameaggregator.core.mapping;

@FunctionalInterface
public interface VendorRequestMapper<R, V> {
    R toInternal(V vendorRequest);
}
