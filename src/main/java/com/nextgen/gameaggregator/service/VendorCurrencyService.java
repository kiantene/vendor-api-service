package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;

public interface VendorCurrencyService {
    VendorCurrency findByVendorIdAndCurrencyId(Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException;

    VendorCurrency findByVendorIdAndVendorCurrencyCode(Integer vendorId, String vendorCurrencyCode) throws VendorCurrencyNotSupportException;
}
