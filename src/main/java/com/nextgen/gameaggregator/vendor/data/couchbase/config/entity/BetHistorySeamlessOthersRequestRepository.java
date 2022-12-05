package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;

import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("log")
@Collection("bet_history_seamless_others")
public interface BetHistorySeamlessOthersRequestRepository extends CouchbaseRepository<BetHistorySeamlessOthersRequest, String> {
}
