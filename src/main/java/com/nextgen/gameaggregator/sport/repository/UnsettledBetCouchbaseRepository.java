package com.nextgen.gameaggregator.sport.repository;

import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
@Collection("sport_unsettled_bet")
public interface UnsettledBetCouchbaseRepository extends CouchbaseRepository<SportUnsettledBetCouchbase, String> {
}
