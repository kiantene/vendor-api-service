package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.RawBetResultRetryLog;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@TypeAlias("bet_result_retry_log")
@Collection("bet_result_retry_log")
public interface RawBetResultRetryLogRepository extends CouchbaseRepository<RawBetResultRetryLog, String> {

    @Query("#{#n1ql.selectEntity} WHERE nextRetryTime < $nextRetryTime AND retryCounter < $retryCounter AND status = $status LIMIT 10")
    List<RawBetResultRetryLog> findByNextRetryTimeLessThanAndRetryCounterLessThanAndStatusEquals(
            @Param("nextRetryTime") Long nextRetryTime,
            @Param("retryCounter") Integer retryCounter,
            @Param("status") Integer status);

}
