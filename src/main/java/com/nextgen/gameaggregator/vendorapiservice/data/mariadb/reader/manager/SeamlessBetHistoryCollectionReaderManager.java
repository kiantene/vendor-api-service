package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.reader.entity.SeamlessBetHistoryCollectionReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeamlessBetHistoryCollectionReaderManager extends JpaRepository<SeamlessBetHistoryCollectionReader,
        Long> {

    SeamlessBetHistoryCollectionReader findByVendorBetId (String vendorBetId);
}
