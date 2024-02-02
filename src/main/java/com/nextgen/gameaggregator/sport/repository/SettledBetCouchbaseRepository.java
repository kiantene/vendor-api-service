package com.nextgen.gameaggregator.sport.repository;

import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
@Collection("sport_settled_bet")
public interface SettledBetCouchbaseRepository extends CouchbaseRepository<SportSettledBet, String> {
}
