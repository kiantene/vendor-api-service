package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.RawBatchProcessIdempotentLog;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@TypeAlias("batch_process_idempotent_log")
@Collection("batch_process_idempotent_log")
public interface RawBatchProcessIdempotentLogRepository extends CouchbaseRepository<RawBatchProcessIdempotentLog, String> {

}
