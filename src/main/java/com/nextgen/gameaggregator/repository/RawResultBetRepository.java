package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawResultBet;
import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("result_bet")
public interface RawResultBetRepository extends CouchbaseRepository<RawResultBet, String> {
    void deleteById(String Id);
}
