package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawBetRefundLog;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("bet_refund_log")
public interface RawBetRefundLogRepository extends CouchbaseRepository<RawBetRefundLog, String> {
}
