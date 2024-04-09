package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.repository.ga.writer.UnsettledBetMariaDBRepository;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import com.nextgen.gameaggregator.sport.repository.UnsettledBetCouchbaseRepository;
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
            throw new BetNotFoundException("Cannot find unsettledBet couchbase Id: " + mergeId);
        }

        return sportUnsettledBetCouchbase;
    }

    public SportUnsettledBetCouchbase idempotentCheck(String vendorPlayerUsername, String externalTransactionId, Integer isConfirmBet) throws BetResultIdempotentViolationException {
        String mergeId = vendorPlayerUsername + '_' + externalTransactionId;
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = null;

        sportUnsettledBetCouchbase = unsettledBetCouchbaseRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBetCouchbase != null) { // No matching bet record
            Integer operatorStatus = sportUnsettledBetCouchbase.getOperatorStatus();
            //TODO TO BE CHANGED TO CREATE_TIME
            Long betTimingDifferenceInMillieSeconds = this.compareWithExistingTimingDifference(sportUnsettledBetCouchbase.getVendorBetTime());

            if (!operatorStatus.equals(ResponseCodes.Status.SC_OK.code) && betTimingDifferenceInMillieSeconds < this.getTimingDifferenceForStillProcessing()) {
                throw new BetResultIdempotentViolationException(sportUnsettledBetCouchbase);

            } else if (operatorStatus.equals(ResponseCodes.Status.SC_OK.code) && sportUnsettledBetCouchbase.getIsConfirmBet() == isConfirmBet) {
                throw new BetResultIdempotentViolationException(sportUnsettledBetCouchbase);

            } else {
                // do nothing
            }
        }

        return sportUnsettledBetCouchbase;
    }

    public Long compareWithExistingTimingDifference(Long createdDate) {

        Long existingTime = System.currentTimeMillis();
        Long timingDifference = existingTime - createdDate;
        return timingDifference;

    }

    public Long getTimingDifferenceForStillProcessing() {
        Long fiveSecondsInMillis = 5L * 1000L;
        return fiveSecondsInMillis;

    }

    public List<VendorGame.SportUnsettledBetMariaDB> mariaDBGetByRoundId(String vendorId, String roundId) throws BetNotFoundException {

        return null;
    }

    public VendorGame.SportUnsettledBetMariaDB mariaDBGetByRoundIdAndVendorBetId(Integer vendorId, String roundId, String vendorBetId) throws BetNotFoundException {

        return null;
    }
}
