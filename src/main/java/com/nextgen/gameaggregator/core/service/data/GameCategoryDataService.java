package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.core.exception.GameCategoryNotFoundException;
import com.nextgen.gameaggregator.entity.ga.GameCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameCategoryDataService {

    private final GameCategoryCacheService cache;

    public GameCategory get(Integer id) {
        return Optional.ofNullable(cache.getById(id))
                .orElseThrow(() -> new GameCategoryNotFoundException("id (" + id + ") cannot be found"));
    }
}
