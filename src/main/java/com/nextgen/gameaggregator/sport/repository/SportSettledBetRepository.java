package com.nextgen.gameaggregator.sport.repository;

import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
@Collection("sport_settled_bet")
public interface SportSettledBetRepository extends CouchbaseRepository<SportSettledBet, String> {
    SportSettledBet findByVendorPlayerUsernameAndVendorBetIdAndRoundId(String vendorPlayerUsername, String vendorBetId, String roundId);

    SportSettledBet findByVendorPlayerUsernameAndVendorBetId(String vendorPlayerUsername, String vendorBetId);


}
