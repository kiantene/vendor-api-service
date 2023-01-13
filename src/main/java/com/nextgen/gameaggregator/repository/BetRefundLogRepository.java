package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.BetRefundLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BetRefundLogRepository extends JpaRepository<BetRefundLog, Long> {

    BetRefundLog findByExternalTransactionIdAndRoundIdAndVendorPlayerId(String externalTransactionId, String roundId, Integer VendorLineId);

}
