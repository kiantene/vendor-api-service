package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorCurrencyRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class VendorCurrencyServiceImpl implements VendorCurrencyService {
    private final VendorCurrencyRepository vendorCurrencyRepository;

    public VendorCurrencyServiceImpl(VendorCurrencyRepository vendorCurrencyRepository) {
        this.vendorCurrencyRepository = vendorCurrencyRepository;
    }

    @Override
    @Cacheable(value = "VendorCurrency", key = "{#vendorId, #currencyId}", cacheManager = "cacheManager")
    public VendorCurrency findByVendorIdAndCurrencyId(Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException {
        VendorCurrency vendorCurrency = vendorCurrencyRepository.findByVendorIdAndCurrencyId(vendorId, currencyId);

        if (vendorCurrency == null || vendorCurrency.getStatus() == 0) {
            throw new VendorCurrencyNotSupportException();
        }

        return vendorCurrency;
    }

    @Override
    @Cacheable(value = "VendorCurrencyCode", key = "{#vendorId, #vendorCurrencyCode}", cacheManager = "cacheManager")
    public VendorCurrency findByVendorIdAndVendorCurrencyCode(Integer vendorId, String vendorCurrencyCode) throws VendorCurrencyNotSupportException {
        VendorCurrency vendorCurrency = vendorCurrencyRepository.findByVendorIdAndVendorCurrencyCode(vendorId, vendorCurrencyCode);

        if (vendorCurrency == null) {
            throw new VendorCurrencyNotSupportException();
        }

        return vendorCurrency;
    }
}
