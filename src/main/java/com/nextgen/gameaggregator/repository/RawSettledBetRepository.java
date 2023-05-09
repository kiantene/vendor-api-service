package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.SettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("settled_bet")
public interface RawSettledBetRepository extends CouchbaseRepository<SettledBet, String> {
    SettledBet findByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId);
}
