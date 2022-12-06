package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorReader;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("log")
@Collection("vendor_player_authentication")
public interface VendorPlayerAuthenticationRepository extends CouchbaseRepository<VendorPlayerAuthentication, String> {
    VendorPlayerAuthentication findByTraceId(String traceId);
}

