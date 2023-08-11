package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawBetAdjustmentLog;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("bet_adjustment_log")
public interface RawBetAdjustmentLogRepository extends CouchbaseRepository<RawBetAdjustmentLog, String> {
}
