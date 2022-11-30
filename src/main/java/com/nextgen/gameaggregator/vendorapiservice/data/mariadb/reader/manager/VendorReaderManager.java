package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;
import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.VendorReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface VendorReaderManager extends JpaRepository<VendorReader, Long> {
//    Optional<VendorReader> findByVendorCode(String vendorCode);

    VendorReader findByVendorCode(String vendorCode);
}

