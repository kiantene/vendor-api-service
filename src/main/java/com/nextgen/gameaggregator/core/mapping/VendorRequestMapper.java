package com.nextgen.gameaggregator.core.mapping;

@FunctionalInterface
public interface VendorRequestMapper<V, I> {
    I toInternal(V vendorRequest);
}
