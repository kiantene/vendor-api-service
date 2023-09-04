package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.SettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("settled_bet_temp")
public interface RawSettledBetRepository extends CouchbaseRepository<SettledBet, String> {
    SettledBet findByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId);

    SettledBet findByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(String vendorBetId, String roundId, Integer vendorId, Long vendorPlayerId);

    SettledBet findByVendorBetIdAndRoundIdAndVendorGameIdAndVendorPlayerId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId);

    List<SettledBet> findByVendorPlayerIdAndRoundId(Long vendorPlayerId, String roundId);
}
