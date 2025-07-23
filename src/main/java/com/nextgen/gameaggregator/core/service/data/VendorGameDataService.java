package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.core.exception.VendorGameNotFoundException;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorGameDataService {

    private final VendorGameCacheService cache;

    public VendorGame getByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) {
        return Optional.ofNullable(cache.getByVendorGameCodeAndVendorId(vendorGameCode, vendorId))
                .orElseThrow(() -> new VendorGameNotFoundException("vendorGameCode, vendorId (" + vendorGameCode + ", " + vendorId + ") cannot be found"));
    }
}
