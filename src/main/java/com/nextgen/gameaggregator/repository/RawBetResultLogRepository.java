package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.RawBetResultLog;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("bet_result_log")
public interface RawBetResultLogRepository extends CouchbaseRepository<RawBetResultLog, String> {

    UnsettledBet findByVendorBetIdAndRoundIdAndVendorGameIdAndVendorPlayerId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId);
    UnsettledBet findByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId);

    List<UnsettledBet> findByRoundId(String roundId, Integer vendorGameId, Long vendorPlayerId);
}
