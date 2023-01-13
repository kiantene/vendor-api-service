package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.BetHistoryCB;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("log")
@Collection("bet_history_CB")
public interface BetHistoryCBRepository extends CouchbaseRepository<BetHistoryCB, String> {

}
