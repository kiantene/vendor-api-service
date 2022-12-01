package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.SeamlessBetHistoryCollectionReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeamlessBetHistoryCollectionReaderManager extends JpaRepository<SeamlessBetHistoryCollectionReader,
        Long> {

    SeamlessBetHistoryCollectionReader findByVendorBetId (String vendorBetId);
}
