package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("unsettled_bet")
public interface RawUnsettledBetRepository extends CouchbaseRepository<UnsettledBet, String> {

    void deleteById(String Id);

    UnsettledBet findByVendorBetIdAndRoundIdAndVendorGameIdAndVendorPlayerId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId);
    UnsettledBet findByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId);

    List<UnsettledBet> findByRoundId(String roundId, Integer vendorGameId, Long vendorPlayerId);
}
