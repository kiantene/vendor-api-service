package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.BetResultLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BetResultLogRepository extends JpaRepository<BetResultLog, String> {
    BetResultLog findByExternalTransactionIdAndVendorGameIdAndVendorPlayerId(String externalTransactionId, Integer vendorGameId, Long vendorPlayerId);

    BetResultLog findByExternalTransactionIdAndRoundIdAndVendorLineId(String externalTransactionId, String roundId, Integer VendorLineId);
}
