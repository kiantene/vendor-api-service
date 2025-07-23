package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.core.exception.VendorPlayerNotFoundException;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorPlayerDataService {
    private final VendorPlayerCacheService cache;

    public VendorPlayer getByUsername(String username) {
        return Optional.ofNullable(cache.getByUsername(username))
                .orElseThrow(() -> new VendorPlayerNotFoundException("username (" + username + ") cannot be found"));
    }
}
