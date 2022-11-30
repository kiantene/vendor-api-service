package com.nextgen.gameaggregator.vendorapiservice.data.couchbase.config.entity;

import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("log")
@Collection("seamless_bet_history_request")
public interface SeamlessBetHistoryRequestRepository extends CouchbaseRepository<SeamlessBetHistoryRequest, String> {

    SeamlessBetHistoryRequest findByVendorBetId (String vendorBetId);
}
