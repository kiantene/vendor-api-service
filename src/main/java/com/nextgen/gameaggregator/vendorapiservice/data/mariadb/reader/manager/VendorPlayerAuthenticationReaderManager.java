package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorPlayerAuthenticationReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPlayerAuthenticationReaderManager extends JpaRepository<VendorPlayerAuthenticationReader, Long> {

    VendorPlayerAuthenticationReader findByTraceId(String traceId);
}
