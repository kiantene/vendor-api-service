package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.core.exception.CurrencyNotFoundException;
import com.nextgen.gameaggregator.entity.ga.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrencyDataService {

    private final CurrencyCacheService cache;

    public Currency get(Integer id) {
        return Optional.ofNullable(cache.getById(id))
                .orElseThrow(() -> new CurrencyNotFoundException("id (" + id + ") cannot be found"));
    }
}
