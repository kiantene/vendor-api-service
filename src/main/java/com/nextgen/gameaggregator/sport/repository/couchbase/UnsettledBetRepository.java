package com.nextgen.gameaggregator.sport.repository.couchbase;

import com.nextgen.gameaggregator.sport.entity.UnsettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("sport")
@Collection("unsettled_bet")
public interface UnsettledBetRepository extends CouchbaseRepository<UnsettledBet, String> {
}
