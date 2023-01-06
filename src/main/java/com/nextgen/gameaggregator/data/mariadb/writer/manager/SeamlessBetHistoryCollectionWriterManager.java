package com.nextgen.gameaggregator.data.mariadb.writer.manager;

import com.nextgen.gameaggregator.data.mariadb.writer.entity.SeamlessBetHistoryCollectionWriter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeamlessBetHistoryCollectionWriterManager extends JpaRepository<SeamlessBetHistoryCollectionWriter,
        Long> {
}
