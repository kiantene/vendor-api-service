package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorPlatformMapReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorPlatformMapReaderManager extends JpaRepository<VendorPlatformMapReader, Long> {

    VendorPlatformMapReader findByVendorIdAndPlatformCode(Long vendorId, String platformCode);
}
