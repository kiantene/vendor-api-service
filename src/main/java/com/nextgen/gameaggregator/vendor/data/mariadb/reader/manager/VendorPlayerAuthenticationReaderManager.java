package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorPlayerAuthenticationReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPlayerAuthenticationReaderManager extends JpaRepository<VendorPlayerAuthenticationReader, Long> {

    VendorPlayerAuthenticationReader findByTraceId(String traceId);
}
