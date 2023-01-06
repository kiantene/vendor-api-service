package com.nextgen.gameaggregator.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.data.mariadb.reader.entity.SeamlessBetHistoryCollectionReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeamlessBetHistoryCollectionReaderManager extends JpaRepository<SeamlessBetHistoryCollectionReader,
        Long> {

    SeamlessBetHistoryCollectionReader findByVendorBetId (String vendorBetId);
}
