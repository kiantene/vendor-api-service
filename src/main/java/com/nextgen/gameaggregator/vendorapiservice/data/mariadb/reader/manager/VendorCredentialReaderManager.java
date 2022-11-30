package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorCredentialReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorCredentialReaderManager extends JpaRepository<VendorCredentialReader, Long> {

    VendorCredentialReader findByIdAndVendorIdAndHouseId(Long id, Long vendorId, Long houseId);
}
