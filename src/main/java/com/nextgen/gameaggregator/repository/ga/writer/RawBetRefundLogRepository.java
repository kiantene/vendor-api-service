package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.RawBetRefundLog;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("bet_refund_logs")
public interface RawBetRefundLogRepository extends CouchbaseRepository<RawBetRefundLog, String> {

    RawBetRefundLog findByVendorPlayerIdAndRoundId(Long vendorPlayerId, String roundId);
}
