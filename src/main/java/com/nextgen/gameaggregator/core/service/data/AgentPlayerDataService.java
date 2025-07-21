package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.core.exception.AgentPlayerNotFoundException;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentPlayerDataService {

    private final AgentPlayerCacheService cache;

    public AgentPlayer get(Long id) {
        return Optional.ofNullable(cache.getById(id))
                .orElseThrow(() -> new AgentPlayerNotFoundException("id (" + id + ") cannot be found"));
    }
}
