package com.nextgen.gameaggregator.vendorapiservice.data.couchbase.entity.seamlessbethistorycollection;

import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Scope("log")
@Collection("seamless_bet_history_collection")
public interface SeamlessBetHistoryCollectionRepository extends CouchbaseRepository<SeamlessBetHistoryCollection, String> {
    Optional<SeamlessBetHistoryCollection> findById(String id);
}
