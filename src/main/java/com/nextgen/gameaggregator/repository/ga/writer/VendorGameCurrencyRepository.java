package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorGameCurrency;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VendorGameCurrencyRepository extends JpaRepository<VendorGameCurrency, Integer> {
    @Cacheable(value = "VendorGameCurrency", key = "{#vendorGameId, #currencyId}", cacheManager = "cacheManager")
    VendorGameCurrency findByVendorGameIdAndCurrencyId(Integer vendorGameId,Integer currencyId);

    @Cacheable(value = "VendorGameCurrency", key = "{#vendorGameId, #currencyId, #status}", cacheManager = "cacheManager")
    VendorGameCurrency findByVendorGameIdAndCurrencyIdAndStatus(Integer vendorGameId,Integer currencyId, Integer status);

    List<VendorGameCurrency> findByVendorGameId(Integer vendorGameId);

}