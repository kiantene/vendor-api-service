package com.nextgen.gameaggregator.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.data.mariadb.reader.entity.VendorCredentialValueReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface VendorCredentialValueReaderManager extends JpaRepository<VendorCredentialValueReader, Long> {

    List<VendorCredentialValueReader> findByVendorCredentialIdAndVersion(Long vendorCredentialId, Long version);
}
