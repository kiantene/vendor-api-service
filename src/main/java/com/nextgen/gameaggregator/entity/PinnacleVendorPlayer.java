package com.nextgen.gameaggregator.entity;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("pinnacle_vendor_username")
@Data
public class PinnacleVendorPlayer {
    @Id
    private String id; // couchbase primary key
    private String vendorPlayerUsername;
    private String username;
}
