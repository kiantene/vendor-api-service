package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCurrencyMapReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorCurrencyMapReaderManager extends JpaRepository<VendorCurrencyMapReader, Long> {

    VendorCurrencyMapReader findByCurrencyCodeAndVendorId(String currencyCode, Long vendorId);
}
