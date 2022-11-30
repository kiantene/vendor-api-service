package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorPlayerReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPlayerReaderManager extends JpaRepository<VendorPlayerReader, Long> {

    VendorPlayerReader findByAgentPlayerIdAndVendorIdAndVendorCredentialIdAndCurrencyCode(Long agentPlayerId, Long vendorId, Long vendorCredentialId, String currencyCode);
}
