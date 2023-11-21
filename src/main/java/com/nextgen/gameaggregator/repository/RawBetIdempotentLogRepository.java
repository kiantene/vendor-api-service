package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawBetIdempotentLog;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@TypeAlias("bet_idempotent_log")
@Collection("bet_idempotent_log")
public interface RawBetIdempotentLogRepository extends CouchbaseRepository<RawBetIdempotentLog, String> {

}
