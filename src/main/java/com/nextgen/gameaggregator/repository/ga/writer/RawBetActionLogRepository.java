package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.RawBetActionLog;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Query;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@TypeAlias("bet_action_log")
@Collection("bet_action_log")
public interface RawBetActionLogRepository extends CouchbaseRepository<RawBetActionLog, String> {
    @Query("#{#n1ql.selectEntity} WHERE nextRetryTime < $nextRetryTime AND retryCounter < $retryCounter")
    List<RawBetActionLog> findTop100ByNextRetryTimeLessThanAndRetryCounterLessThan(
            @Param("nextRetryTime") long nextRetryTime,
            @Param("retryCounter") int retryCounter);
}
