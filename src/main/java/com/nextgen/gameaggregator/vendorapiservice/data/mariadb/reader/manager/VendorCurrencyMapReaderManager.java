package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorCurrencyMapReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorCurrencyMapReaderManager extends JpaRepository<VendorCurrencyMapReader, Long> {

    VendorCurrencyMapReader findByCurrencyCodeAndVendorId(String currencyCode, Long vendorId);
}
