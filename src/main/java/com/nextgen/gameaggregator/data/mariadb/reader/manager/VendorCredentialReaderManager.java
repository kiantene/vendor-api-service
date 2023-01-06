package com.nextgen.gameaggregator.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.data.mariadb.reader.entity.VendorCredentialReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorCredentialReaderManager extends JpaRepository<VendorCredentialReader, Long> {

    VendorCredentialReader findByIdAndVendorIdAndHouseId(Long id, Long vendorId, Long houseId);
}
