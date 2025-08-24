package com.nextgen.gameaggregator.core.security.signature;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VendorSignatureValidatorRegistry {
    private final Map<String, VendorSignatureValidator> validatorMap;

    public VendorSignatureValidatorRegistry(List<VendorSignatureValidator> validators) {
        this.validatorMap = validators.stream()
                .filter(h -> h.getVendorClassName() != null)
                .collect(Collectors.toMap(
                        VendorSignatureValidator::getVendorClassName,
                        Function.identity()
                ));
    }

    public VendorSignatureValidator getValidator(String vendorClassName) {
        return validatorMap.get(vendorClassName);
    }
}
