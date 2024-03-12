package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnsettledBetMariaDBRepository extends JpaRepository<VendorGame.SportUnsettledBetMariaDB, String> {
    List<VendorGame.SportUnsettledBetMariaDB> findByExternalTransactionIdAndRoundIdAndVendorLineId(String externalTransactionId, String roundId, Integer VendorLineId);
}
