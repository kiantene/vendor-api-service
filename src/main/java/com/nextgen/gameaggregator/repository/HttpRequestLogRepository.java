package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("http_request_logs")
public interface HttpRequestLogRepository extends CouchbaseRepository<HttpRequestLog, String> {

}
