package com.nextgen.gameaggregator.vendor.data.couchbase.entity.traceidlogrequest;

import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("log")
@Collection("traceid_request")
public interface TraceIdLogRepository extends CouchbaseRepository<TraceIdLogRequest, String> {
}
