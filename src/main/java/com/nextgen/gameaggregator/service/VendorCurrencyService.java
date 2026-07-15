package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;

public interface VendorCurrencyService {
    VendorCurrency findByVendorIdAndCurrencyId(Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException;

    VendorCurrency findByVendorIdAndVendorCurrencyCode(Integer vendorId, String vendorCurrencyCode) throws VendorCurrencyNotSupportException;

    /**
     * The VENDOR-FACING currency code for (vendorId, currencyId) — what a vendor expects on the wire,
     * distinct from GA's internal currency code. Resolving this is a currency-service responsibility, so
     * callers should not re-implement `findByVendorIdAndCurrencyId(...).getVendorCurrencyCode()` inline.
     */
    default String getVendorCurrencyCode(Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException {
        return findByVendorIdAndCurrencyId(vendorId, currencyId).getVendorCurrencyCode();
    }
}
