package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.sport.repository.UnsettledBetCouchbaseRepository;
import com.nextgen.gameaggregator.sport.repository.UnsettledBetMariaDBRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SportUnsettledBetService {

    @Autowired
    private UnsettledBetMariaDBRepository unsettledBetMariaDBRepository;
    @Autowired
    private UnsettledBetCouchbaseRepository unsettledBetCouchbaseRepository;

    public SportUnsettledBetCouchbase save(SportUnsettledBetCouchbase sportUnsettledBetCouchbase) {
        unsettledBetCouchbaseRepository.save(sportUnsettledBetCouchbase);
        return sportUnsettledBetCouchbase;
    }

    public void delete(SportUnsettledBetCouchbase sportUnsettledBetCouchbase) {
        unsettledBetCouchbaseRepository.delete(sportUnsettledBetCouchbase);
    }

    public SportUnsettledBetCouchbase couchbaseGetByExternalTransactionId(String vendorId, String vendorGameId, String vendorPlayerId, String externalTransactionId) throws BetNotFoundException {
        String mergeId = vendorId + '_' + vendorGameId + '_' + vendorPlayerId + '_' + externalTransactionId;
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = null;

        sportUnsettledBetCouchbase = unsettledBetCouchbaseRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBetCouchbase == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find couchbase Id: " + mergeId);
        }

        return sportUnsettledBetCouchbase;
    }

    public List<SportUnsettledBetMariaDB> mariaDBGetByRoundId(String vendorId, String roundId) throws BetNotFoundException {

        return null;
    }

    public SportUnsettledBetMariaDB mariaDBGetByRoundIdAndVendorBetId(String vendorCode, String roundId, String vendorBetId) throws BetNotFoundException {

        return null;
    }
}
