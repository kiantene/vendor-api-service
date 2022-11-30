package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.writer.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.writer.entity.VendorPlayerWriter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPlayerWriterManager extends JpaRepository<VendorPlayerWriter, Long> {
}
