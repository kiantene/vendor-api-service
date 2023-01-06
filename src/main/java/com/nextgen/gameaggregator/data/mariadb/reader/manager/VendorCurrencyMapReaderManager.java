package com.nextgen.gameaggregator.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.data.mariadb.reader.entity.VendorCurrencyMapReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorCurrencyMapReaderManager extends JpaRepository<VendorCurrencyMapReader, Long> {

    VendorCurrencyMapReader findByCurrencyCodeAndVendorId(String currencyCode, Long vendorId);
}
