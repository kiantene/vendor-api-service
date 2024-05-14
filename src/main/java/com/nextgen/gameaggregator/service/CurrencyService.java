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

    @Autowired
    private CurrencyRepository currencyRepository;


    @Cacheable(value = "Currencies", key = "#currencyId", cacheManager = "cacheManager")
    public Currency getByCurrencyId(Integer currencyId, Currency currency) throws InvalidCurrencyException {
        if (currency == null) {
            currency = currencyRepository.findById(currencyId).orElse(null);
            Optional.ofNullable(currency).orElseThrow(InvalidCurrencyException::new);
        }

        return currency;
    }
}
