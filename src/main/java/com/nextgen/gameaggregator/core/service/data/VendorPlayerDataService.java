package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorPlayerDataService {
    private final VendorPlayerCacheService cache;

    public VendorPlayer getByUsername(String username) throws InvalidPlayerException {
        return Optional.ofNullable(cache.getByUsername(username))
                .orElseThrow(InvalidPlayerException::new); // TODO: throw VendorPlayerNotFoundException
    }
}
