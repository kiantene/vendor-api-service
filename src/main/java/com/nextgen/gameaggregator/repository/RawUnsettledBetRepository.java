package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawUnsettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("unsettled_bet")
public interface RawUnsettledBetRepository extends CouchbaseRepository<RawUnsettledBet, String> {

    void deleteById(String Id);

    RawUnsettledBet findByVendorBetIdAndRoundIdAndVendorGameIdAndVendorPlayerId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId);
}
