package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class VendorPlayerCacheService {
    private final VendorPlayerRepository repository;

    public VendorPlayer getByUsername(String username) {
        return repository.findByUsername(username);
    }
}
