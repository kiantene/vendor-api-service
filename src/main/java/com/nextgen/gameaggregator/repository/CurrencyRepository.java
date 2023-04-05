package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
    Currency findByCode(String code);

    @Override
    List<Currency> findAll();
}

