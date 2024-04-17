package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
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

    public SportUnsettledBetCouchbase getByVendorPlayerUsernameAndRoundId(String vendorPlayerUsername, String roundId) throws BetNotFoundException {
        String mergeId = vendorPlayerUsername + '_' + roundId;
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = null;

        sportUnsettledBetCouchbase = unsettledBetCouchbaseRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBetCouchbase == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find Id from SportUnsettledBetCouchbase getByVendorPlayerUsernameAndRoundId: " + mergeId);
        }

        return sportUnsettledBetCouchbase;
    }

    public SportUnsettledBetCouchbase idempotentCheck(String vendorPlayerUsername, String roundId, String externalTransactionId) throws BetResultIdempotentViolationException, TransactionStillProcessingException {
        String mergeId = vendorPlayerUsername + '_' + roundId;
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = null;

        sportUnsettledBetCouchbase = unsettledBetCouchbaseRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBetCouchbase != null) {
            Long betTimingDifferenceInMillieSeconds = this.compareWithExistingTimingDifference(sportUnsettledBetCouchbase.getVendorBetTime());

            if (sportUnsettledBetCouchbase.getExternalTransactionId().equals(externalTransactionId)) {
                if (sportUnsettledBetCouchbase.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                    throw new BetResultIdempotentViolationException(sportUnsettledBetCouchbase);

                } else if (betTimingDifferenceInMillieSeconds < this.getTimingDifferenceForStillProcessing()) {
                    throw new TransactionStillProcessingException("SportUnsettledBetCouchbase idempotentCheck : " + betTimingDifferenceInMillieSeconds + " seconds.");

                } else {
                    //do nothing when externalTransactionId is matched, but status is not OK, we will resend the request to operator
                }
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
