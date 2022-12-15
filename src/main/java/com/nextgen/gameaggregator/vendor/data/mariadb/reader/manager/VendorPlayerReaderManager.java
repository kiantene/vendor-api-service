package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorPlayerReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorPlayerReaderManager extends JpaRepository<VendorPlayerReader, Long> {

    VendorPlayerReader findByAgentPlayerIdAndVendorIdAndVendorCredentialIdAndCurrencyCode(Long agentPlayerId, Long vendorId, Long vendorCredentialId, String currencyCode);

    VendorPlayerReader findByVendorUsername(String vendorUsername);
}
