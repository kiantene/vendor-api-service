package com.nextgen.gameaggregator.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.data.mariadb.reader.entity.VendorLanguageMapReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorLanguageMapReaderManager extends JpaRepository<VendorLanguageMapReader, Long> {

    VendorLanguageMapReader findByVendorIdAndIsDefaultLanguage(Long vendorId, Boolean isDefault);

    VendorLanguageMapReader findByVendorIdAndLanguageCode(Long vendorId, String languageCode);
}
