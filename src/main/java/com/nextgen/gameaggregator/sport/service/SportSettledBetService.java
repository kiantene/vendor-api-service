package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.repository.SportSettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class SportSettledBetService {

    @Autowired
    private SportSettledBetRepository sportSettledBetRepository;

    public SportSettledBet save(SportSettledBet sportSettledBet) {
        sportSettledBetRepository.save(sportSettledBet);
        return sportSettledBet;
    }

    public SportSettledBet getByExternalTransactionId(String vendorPlayerUsername, String externalTransactionId) throws BetNotFoundException {
        String mergeId = vendorPlayerUsername + '_' + externalTransactionId;
        SportSettledBet sportSettledBet = null;

        sportSettledBet = sportSettledBetRepository.findById(mergeId).orElse(null);
        if (sportSettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find sportSettledBet couchbase Id: " + mergeId);
        }

        return sportSettledBet;
    }

    public SportSettledBet getByRoundId(String vendorPlayerUsername, String roundId) throws BetNotFoundException {
        String mergeId = vendorPlayerUsername + '_' + roundId;
        SportSettledBet sportSettledBet = null;

        sportSettledBet = sportSettledBetRepository.findById(mergeId).orElse(null);
        if (sportSettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find sportSettledBet couchbase Id: " + mergeId);
        }

        return sportSettledBet;
    }

    public SportSettledBet getByVendorPlayerUsernameAndVendorBetIdAndRoundId(String vendorPlayerUsername, String vendorBetId, String roundId) throws BetNotFoundException {
        SportSettledBet sportSettledBet = null;

        sportSettledBet = sportSettledBetRepository.findByVendorPlayerUsernameAndVendorBetIdAndRoundId(vendorPlayerUsername, vendorBetId, roundId);
        if (sportSettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find sportSettledBet getByVendorPlayerUsernameAndVendorBetIdAndRoundId: " + vendorPlayerUsername + ", " + vendorBetId + ", " + roundId);
        }

        return sportSettledBet;
    }

    public SportSettledBet getByVendorPlayerUsernameAndVendorBetId(String vendorPlayerUsername, String vendorBetId) throws BetNotFoundException {
        SportSettledBet sportSettledBet = null;

        sportSettledBet = sportSettledBetRepository.findByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

        if (Objects.isNull(sportSettledBet)) {
            sportSettledBet = sportSettledBetRepository.findById(vendorPlayerUsername + '_' + vendorBetId).orElse(null);
        }
        
        if (sportSettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find sportSettledBet getByVendorPlayerUsernameAndVendorBetId: " + vendorPlayerUsername + ", " + vendorBetId);
        }

        return sportSettledBet;
    }

    public void delete(SportSettledBet sportSettledBet) {
        sportSettledBetRepository.delete(sportSettledBet);
    }
}
