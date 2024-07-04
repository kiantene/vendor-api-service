package com.nextgen.gameaggregator.sport.repository;

import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
@Collection("sport_unsettled_bet")
public interface SportUnsettledBetRepository extends CouchbaseRepository<SportUnsettledBet, String> {
    SportUnsettledBet findByVendorPlayerUsernameAndVendorBetIdAndRoundId(String vendorPlayerUsername, String vendorBetId, String roundId);

    SportUnsettledBet findByVendorPlayerUsernameAndVendorBetId(String vendorPlayerUsername, String vendorBetId);


}
