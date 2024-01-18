package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.BetNotFoundLog;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("bet_not_found_log")
public interface RawBetNotFoundLogRepository extends CouchbaseRepository<BetNotFoundLog, String> {
    BetNotFoundLog findByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId);
    
}
