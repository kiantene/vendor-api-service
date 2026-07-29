package com.nextgen.gameaggregator.core.registry;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VendorResponseProcessorRegistry {
    private final Map<String, VendorResponsePostProcessor> responsePostProcessorMap;

    public VendorResponseProcessorRegistry(List<VendorResponsePostProcessor> responsePostProcessors) {
        this.responsePostProcessorMap = responsePostProcessors.stream()
                .filter(h -> h.getVendorClassName() != null)
                .collect(Collectors.toMap(VendorResponsePostProcessor::getVendorClassName, Function.identity()));
    }

    public VendorResponsePostProcessor get(String vendorClassName) {
        return responsePostProcessorMap.get(vendorClassName);
    }

}
