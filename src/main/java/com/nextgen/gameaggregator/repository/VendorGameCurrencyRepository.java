package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGameCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VendorGameCurrencyRepository extends JpaRepository<VendorGameCurrency, Integer> {
    VendorGameCurrency findByVendorGameIdAndCurrencyId(Integer vendorGameId,Integer currencyId);

    VendorGameCurrency findByVendorGameIdAndCurrencyIdAndStatus(Integer vendorGameId,Integer currencyId, Integer status);

    List<VendorGameCurrency> findByVendorGameId(Integer vendorGameId);

}