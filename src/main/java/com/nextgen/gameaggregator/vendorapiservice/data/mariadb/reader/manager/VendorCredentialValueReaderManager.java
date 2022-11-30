package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorCredentialValueReader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorCredentialValueReaderManager extends JpaRepository<VendorCredentialValueReader, Long> {

    List<VendorCredentialValueReader> findByVendorCredentialIdAndVersion(Long vendorCredentialId, Long version);
}
