package com.nextgen.gameaggregator.repository.wallet.reader;

import com.nextgen.gameaggregator.entity.wallet.TransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferHistoryReaderRepository extends JpaRepository<TransferHistory, String> {

    TransferHistory findTransferHistoriesByAgentIdAndReferenceId(Integer agentId, String referenceId);
}
