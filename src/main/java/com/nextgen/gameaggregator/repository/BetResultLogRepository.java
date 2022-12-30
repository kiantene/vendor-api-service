package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.BetResultLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BetResultLogRepository extends JpaRepository<BetResultLog, String> {
    BetResultLog findByExternalTransactionIdAndVendorGameIdAndVendorPlayerId(String externalTransactionId, Integer vendorGameId, Long vendorPlayerId);
}
