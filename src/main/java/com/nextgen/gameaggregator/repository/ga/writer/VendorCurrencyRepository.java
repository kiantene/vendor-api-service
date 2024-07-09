package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorCurrencyRepository extends JpaRepository<VendorCurrency, Integer> {
    List<VendorCurrency> findByVendorId(Integer vendorId);

    @Cacheable(value = "VendorCurrencies", key = "{#vendorId, #currencyId}", cacheManager = "cacheManager")
    VendorCurrency findByVendorIdAndCurrencyId(Integer vendorId, Integer currencyId);

    VendorCurrency findByVendorIdAndVendorCurrencyCode(Integer vendorId, String vendorCurrencyCode);
}
