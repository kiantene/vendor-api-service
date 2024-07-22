package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@TypeAlias("request_idempotent_log")
@Collection("request_idempotent_log")
public interface RequestIdempotentLogRepository extends CouchbaseRepository<RequestIdempotentLog, String> {

}
