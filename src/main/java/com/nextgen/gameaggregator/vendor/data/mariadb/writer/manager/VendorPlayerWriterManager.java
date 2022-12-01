package com.nextgen.gameaggregator.vendor.data.mariadb.writer.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.VendorPlayerWriter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPlayerWriterManager extends JpaRepository<VendorPlayerWriter, Long> {
}
