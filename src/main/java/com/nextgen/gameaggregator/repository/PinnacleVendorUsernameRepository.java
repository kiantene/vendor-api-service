package com.nextgen.gameaggregator.repository;

import java.util.Optional;

import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

import com.nextgen.gameaggregator.entity.PinnacleVendorPlayer;

@Repository
@Scope("raw")
@Collection("pinnacle_vendor_username")
public interface PinnacleVendorUsernameRepository extends CouchbaseRepository<PinnacleVendorPlayer, String> {
    Optional<PinnacleVendorPlayer> findByUsername(String username);
}
