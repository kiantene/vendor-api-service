package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.BetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BetHistoryRepository extends JpaRepository<BetHistory, String> {
    BetHistory findByRoundIdAndVendorGameIdAndVendorPlayerId(String roundId, Integer vendorGameId, Long vendorPlayerId);

    BetHistory findByExternalTransactionIdAndVendorId(String txnId, Integer vendorId);
}
