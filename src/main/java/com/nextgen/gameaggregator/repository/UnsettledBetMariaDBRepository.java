package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.SportUnsettledBetMariaDB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnsettledBetMariaDBRepository extends JpaRepository<SportUnsettledBetMariaDB, String> {
    SportUnsettledBetMariaDB findByExternalTransactionIdAndRoundIdAndVendorLineId(String externalTransactionId, String roundId, Integer VendorLineId);
}
