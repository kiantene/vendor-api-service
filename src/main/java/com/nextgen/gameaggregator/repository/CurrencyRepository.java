package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.Currency;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {

    @Cacheable(value = "Currencies", key = "#currencyCode" , cacheManager = "cacheManager")
    Currency findByCode(String currencyCode);

    @Override
    List<Currency> findAll();
}

