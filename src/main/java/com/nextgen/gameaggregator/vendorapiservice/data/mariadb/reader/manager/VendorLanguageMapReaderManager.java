package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorLanguageMapReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorLanguageMapReaderManager extends JpaRepository<VendorLanguageMapReader, Long> {

    VendorLanguageMapReader findByVendorIdAndIsDefaultLanguage(Long vendorId, Boolean isDefault);

    VendorLanguageMapReader findByVendorIdAndLanguageCode(Long vendorId, String languageCode);
}
