package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorGameLanguageMapReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorGameLanguageMapReaderManager extends JpaRepository<VendorGameLanguageMapReader, Long> {

    VendorGameLanguageMapReader findByVendorGameIdAndLanguageCodeAndPlatformCode(Long vendorGameId, String languageCode, String platformCode);
}
