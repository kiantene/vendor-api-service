package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.exception.InvalidCurrencyException;
import com.nextgen.gameaggregator.repository.ga.writer.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrencyService {
    private final CurrencyRepository currencyRepository;

    @Autowired
    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Cacheable(value = "Currencies", key = "#currencyId", cacheManager = "cacheManager")
    public Currency get(Integer currencyId) throws InvalidCurrencyException {
        return currencyRepository.findById(currencyId).orElseThrow(InvalidCurrencyException::new);
    }

    @Cacheable(value = "Currencies", key = "#currencyId", cacheManager = "cacheManager")
    public Currency getByCurrencyId(Integer currencyId, Currency currency) throws InvalidCurrencyException {
        return currency == null ? this.get(currencyId) : currency;
    }

    public Currency getByCode(String code) throws InvalidCurrencyException {
        Currency currency = currencyRepository.findByCode(code);
        return Optional.ofNullable(currency).orElseThrow(InvalidCurrencyException::new);
    }
}
