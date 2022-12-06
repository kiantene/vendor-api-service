package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;

import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("log")
@Collection("seamless_refund_log")
public interface SeamlessRefundLogRequestRepository extends CouchbaseRepository <SeamlessRefundLogRequest, String>{

    SeamlessRefundLogRequest findByVendorBetIdAndVendorCode (String vendorBetId, String vendorCode);
}
