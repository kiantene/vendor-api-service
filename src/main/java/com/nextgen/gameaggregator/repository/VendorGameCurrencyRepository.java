package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorGameCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface VendorGameCurrencyRepository extends JpaRepository<VendorGameCurrency, Integer> {
    VendorGameCurrency findByVendorGameIdAndCurrencyId(Integer vendorGameId,Integer currencyId);

}