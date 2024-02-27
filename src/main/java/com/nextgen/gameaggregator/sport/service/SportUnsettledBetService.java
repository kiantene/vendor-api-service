package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import com.nextgen.gameaggregator.sport.repository.UnsettledBetCouchbaseRepository;
import com.nextgen.gameaggregator.repository.ga.writer.UnsettledBetMariaDBRepository;
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

    public SportUnsettledBetCouchbase couchbaseGetByExternalTransactionId(String vendorPlayerUsername, String externalTransactionId) throws BetNotFoundException {
        String mergeId = vendorPlayerUsername + '_' + externalTransactionId;
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = null;

        sportUnsettledBetCouchbase = unsettledBetCouchbaseRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBetCouchbase == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find couchbase Id: " + mergeId);
        }

        return sportUnsettledBetCouchbase;
    }

    public void idempotentCheck(String vendorPlayerUsername, String externalTransactionId) throws BetResultIdempotentViolationException {
        String mergeId = vendorPlayerUsername + '_' + externalTransactionId;
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = null;

        sportUnsettledBetCouchbase = unsettledBetCouchbaseRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBetCouchbase != null) { // No matching bet record
            throw new BetResultIdempotentViolationException(sportUnsettledBetCouchbase);
        }
    }

    public List<VendorGame.SportUnsettledBetMariaDB> mariaDBGetByRoundId(String vendorId, String roundId) throws BetNotFoundException {

        return null;
    }

    public VendorGame.SportUnsettledBetMariaDB mariaDBGetByRoundIdAndVendorBetId(Integer vendorId, String roundId, String vendorBetId) throws BetNotFoundException {

        return null;
    }
}
