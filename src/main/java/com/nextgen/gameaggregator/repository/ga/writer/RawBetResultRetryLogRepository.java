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

    List<RawBetResultRetryLog> findTop10ByRetryCounterAndNextRetryTimeAndStatus(Integer retryCounter, Long nextRetryTime, Integer status);

    @Query(value = "SELECT * FROM bet_result_retry_log WHERE retryCounter <= :retryCounter AND nextRetryTime <= :nextRetryTime AND status = :status LIMIT :limit", nativeQuery = true)
    List<RawBetResultRetryLog> findByRetryCounterAndNextRetryTimeAndStatusAndLimit(@Param("retryCounter") Integer retryCounter, @Param("nextRetryTime") Long nextRetryTime, @Param("status") Integer status, @Param("limit") Integer limit);
}
