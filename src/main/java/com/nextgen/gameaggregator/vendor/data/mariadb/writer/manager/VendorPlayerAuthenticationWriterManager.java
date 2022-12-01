package com.nextgen.gameaggregator.vendor.data.mariadb.writer.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.VendorPlayerAuthenticationWriter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPlayerAuthenticationWriterManager extends JpaRepository<VendorPlayerAuthenticationWriter, Long> {
}
