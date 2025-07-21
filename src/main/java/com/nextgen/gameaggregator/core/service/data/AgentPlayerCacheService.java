package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AgentPlayerCacheService {
    private final AgentPlayerRepository repository;

    public AgentPlayer getById(Long id) {
        return repository.findById(id).orElse(null);
    }
}
