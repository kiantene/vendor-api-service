package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorReaderManager extends JpaRepository<VendorReader, Long> {
//    Optional<VendorReader> findByVendorCode(String vendorCode);

    VendorReader findByVendorCode(String vendorCode);
}

