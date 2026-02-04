package com.nextgen.gameaggregator.repository.ga.writer;

import com.couchbase.client.java.query.QueryScanConsistency;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import org.springframework.data.couchbase.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Scope("raw")
@Collection("settled_bets")
public interface RawSettledBetRepository extends CouchbaseRepository<SettledBet, String> {
    SettledBet findByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId);

    List<SettledBet> findByVendorPlayerIdAndRoundId(Long vendorPlayerId, String roundId);

    @Query("#{#n1ql.selectEntity} WHERE vendorPlayerId = $vendorPlayerId AND externalTransactionId = $externalTransactionId")
    @ScanConsistency(query = QueryScanConsistency.REQUEST_PLUS)
    SettledBet findByVendorPlayerIdAndExternalTransactionIdWithRequestPlus(
            @Param("vendorPlayerId") Long vendorPlayerId,
            @Param("externalTransactionId") String externalTransactionId
    );
}
