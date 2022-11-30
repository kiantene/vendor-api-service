package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorGameLanguageMapReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorGameLanguageMapReaderManager extends JpaRepository<VendorGameLanguageMapReader, Long> {

    VendorGameLanguageMapReader findByVendorGameIdAndLanguageCodeAndPlatformCode(Long vendorGameId, String languageCode, String platformCode);
}
