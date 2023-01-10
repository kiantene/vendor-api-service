package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.VendorLineCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorLineCurrencyRepository  extends JpaRepository<VendorLineCurrency, Integer> {
    VendorLineCurrency findByVendorLineIdAndCurrencyIdAndStatus(Integer vendorLineId, Integer currencyId, Integer status);
}

