package com.nextgen.gameaggregator.vendor.data.mariadb.writer.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.writer.entity.SeamlessBetHistoryCollectionWriter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeamlessBetHistoryCollectionWriterManager extends JpaRepository<SeamlessBetHistoryCollectionWriter,
        Long> {
}
