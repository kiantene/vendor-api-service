package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.repository.SettledBetCouchbaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SportSettledBetService {

    @Autowired
    private SettledBetCouchbaseRepository settledBetCouchbaseRepository;

    public SportSettledBet save(SportSettledBet sportSettledBet) {
        settledBetCouchbaseRepository.save(sportSettledBet);
        return sportSettledBet;
    }

    public SportSettledBet getByExternalTransactionId(String vendorPlayerUsername, String externalTransactionId) throws BetNotFoundException {
        String mergeId = vendorPlayerUsername + '_' + externalTransactionId;
        SportSettledBet sportSettledBet = null;

        sportSettledBet = settledBetCouchbaseRepository.findById(mergeId).orElse(null);
        if (sportSettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find couchbase Id: " + mergeId);
        }

        return sportSettledBet;
    }

    public void delete(SportSettledBet sportSettledBet) {
        settledBetCouchbaseRepository.delete(sportSettledBet);
    }
}
