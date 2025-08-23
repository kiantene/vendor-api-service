package com.nextgen.gameaggregator.core.decrypter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VendorDecrypterRegistry {
    private final Map<String, VendorDecrypter> decrypterMap;

    public VendorDecrypterRegistry(List<VendorDecrypter> decrypters) {
        this.decrypterMap = decrypters.stream()
                .filter(h -> h.getVendorClassName() != null)
                .collect(Collectors.toMap(VendorDecrypter::getVendorClassName, Function.identity()));
    }

    public VendorDecrypter get(String vendorClassName) {
        return decrypterMap.get(vendorClassName);
    }
}
