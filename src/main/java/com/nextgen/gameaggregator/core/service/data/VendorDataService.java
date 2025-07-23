package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.core.exception.VendorNotFoundException;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorDataService {

    private final VendorCacheService cache;

    public Vendor get(Integer id) {
        return Optional.ofNullable(cache.getById(id))
                .orElseThrow(() -> new VendorNotFoundException("id (" + id + ") cannot be found"));
    }
}
