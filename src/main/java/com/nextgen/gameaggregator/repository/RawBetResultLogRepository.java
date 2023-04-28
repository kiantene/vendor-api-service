package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawBetResultLog;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("bet_result_log")
public interface RawBetResultLogRepository extends CouchbaseRepository<RawBetResultLog, String> {
}
