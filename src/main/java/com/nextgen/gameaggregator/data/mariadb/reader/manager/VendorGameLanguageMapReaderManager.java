package com.nextgen.gameaggregator.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.data.mariadb.reader.entity.VendorGameLanguageMapReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorGameLanguageMapReaderManager extends JpaRepository<VendorGameLanguageMapReader, Long> {

    VendorGameLanguageMapReader findByVendorGameIdAndLanguageCodeAndPlatformCode(Long vendorGameId, String languageCode, String platformCode);

    List<VendorGameLanguageMapReader> findByVendorBetGameCodeAndVendorId(String vendorBetGameCode, Long vendorId);
}
