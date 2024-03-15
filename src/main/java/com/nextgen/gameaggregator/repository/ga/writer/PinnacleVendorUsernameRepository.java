package com.nextgen.gameaggregator.repository.ga.writer;

import java.util.Optional;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope("raw")
@Collection("pinnacle_vendor_username")
public interface PinnacleVendorUsernameRepository extends CouchbaseRepository<VendorGame.PinnacleVendorPlayer, String> {
    Optional<VendorGame.PinnacleVendorPlayer> findByUsername(String username);

    Optional<VendorGame.PinnacleVendorPlayer> findByVendorPlayerUsername(String vendorPlayerUsername);
}
