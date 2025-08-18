package com.nextgen.gameaggregator.core.exception.mapper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VendorExceptionMapperRegistry {
    private final Map<String, VendorExceptionMapper> handlerMap;

    public VendorExceptionMapperRegistry(List<VendorExceptionMapper> handlers) {
        this.handlerMap = handlers.stream()
                .filter(h -> h.getVendorClassName() != null)
                .collect(Collectors.toMap(
                        VendorExceptionMapper::getVendorClassName,
                        Function.identity()
                ));
    }

    public VendorExceptionMapper getMapper(String vendorClassName) {
        return handlerMap.get(vendorClassName);
    }
}
