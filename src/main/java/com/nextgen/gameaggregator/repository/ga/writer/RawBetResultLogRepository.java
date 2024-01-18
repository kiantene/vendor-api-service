package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.RawBetResultLog;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@TypeAlias("bet_result_logs")
@Collection("bet_result_logs")
public interface RawBetResultLogRepository extends CouchbaseRepository<RawBetResultLog, String> {
}
