package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorPlatformMapReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPlatformMapReaderManager extends JpaRepository<VendorPlatformMapReader, Long> {

    VendorPlatformMapReader findByVendorIdAndPlatformCode(Long vendorId, String platformCode);
}
