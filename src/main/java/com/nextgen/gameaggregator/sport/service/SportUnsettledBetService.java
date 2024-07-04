package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.repository.SportUnsettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class SportUnsettledBetService {

    private final SportUnsettledBetRepository sportUnsettledBetRepository;

    @Autowired
    public SportUnsettledBetService(SportUnsettledBetRepository sportUnsettledBetRepository) {
        this.sportUnsettledBetRepository = sportUnsettledBetRepository;
    }

    public SportUnsettledBet save(SportUnsettledBet sportUnsettledBet) {
        sportUnsettledBetRepository.save(sportUnsettledBet);
        return sportUnsettledBet;
    }

    public Optional<SportUnsettledBet> getById(String id) {
        return sportUnsettledBetRepository.findById(id);
    }

    public void delete(SportUnsettledBet sportUnsettledBet) {
        sportUnsettledBetRepository.delete(sportUnsettledBet);
    }

    public SportUnsettledBet couchbaseGetByExternalTransactionId(String vendorPlayerUsername, String externalTransactionId) throws BetNotFoundException {
        String mergeId = vendorPlayerUsername + '_' + externalTransactionId;
        SportUnsettledBet sportUnsettledBet = null;

        sportUnsettledBet = sportUnsettledBetRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find unsettledBet couchbase Id: " + mergeId);
        }

        return sportUnsettledBet;
    }

    public SportUnsettledBet getByVendorPlayerUsernameAndRoundId(String vendorPlayerUsername, String roundId) throws BetNotFoundException {
        String mergeId = vendorPlayerUsername + '_' + roundId;
        SportUnsettledBet sportUnsettledBet = null;

        sportUnsettledBet = sportUnsettledBetRepository.findById(mergeId).orElse(null);
        if (sportUnsettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find Id from SportUnsettledBetCouchbase getByVendorPlayerUsernameAndRoundId: " + mergeId);
        }

        return sportUnsettledBet;
    }

    public SportUnsettledBet getByVendorPlayerUsernameAndVendorBetIdAndRoundId(String vendorPlayerUsername, String vendorBetId, String roundId) throws BetNotFoundException {
        SportUnsettledBet sportUnsettledBet = null;

        sportUnsettledBet = sportUnsettledBetRepository.findByVendorPlayerUsernameAndVendorBetIdAndRoundId(vendorPlayerUsername, vendorBetId, roundId);
        if (sportUnsettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find Id from SportUnsettledBetCouchbase getByVendorPlayerUsernameAndVendorBetIdAndRoundId: " + vendorPlayerUsername + ", " + vendorBetId + ", " + roundId);
        }

        return sportUnsettledBet;
    }

    public SportUnsettledBet getByVendorPlayerUsernameAndVendorBetId(String vendorPlayerUsername, String vendorBetId) throws BetNotFoundException {
        SportUnsettledBet sportUnsettledBet = null;

        sportUnsettledBet = sportUnsettledBetRepository.findByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
        if (sportUnsettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find Id from SportUnsettledBetCouchbase getByVendorPlayerUsernameAndVendorBetId: " + vendorPlayerUsername + ", " + vendorBetId);
        }

        return sportUnsettledBet;
    }

//    public SportUnsettledBet idempotentCheck(String vendorPlayerUsername, String roundId, String externalTransactionId) throws BetResultIdempotentViolationException, TransactionStillProcessingException {
//        String mergeId = vendorPlayerUsername + '_' + roundId;
//        SportUnsettledBet sportUnsettledBet = null;
//
//        sportUnsettledBet = sportUnsettledBetRepository.findById(mergeId).orElse(null);
//        if (sportUnsettledBet != null) {
//            Long betTimingDifferenceInMillieSeconds = this.compareWithExistingTimingDifference(sportUnsettledBet.getVendorBetTime());
//
//            if (sportUnsettledBet.getExternalTransactionId().equals(externalTransactionId)) {
//                if (sportUnsettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
//                    throw new BetResultIdempotentViolationException(sportUnsettledBet);
//
//                } else if (betTimingDifferenceInMillieSeconds < this.getTimingDifferenceForStillProcessing()) {
//                    throw new TransactionStillProcessingException("SportUnsettledBetCouchbase idempotentCheck : " + betTimingDifferenceInMillieSeconds + " seconds.");
//
//                } else {
//                    //do nothing when externalTransactionId is matched, but status is not OK, we will resend the request to operator
//                }
//            }
//        }
//
//        return sportUnsettledBet;
//    }

    public SportUnsettledBet idempotentCheck(String vendorPlayerUsername, String vendorBetId, String externalTransactionId) throws BetResultIdempotentViolationException, TransactionStillProcessingException {
        WalletRequest walletRequest = new WalletRequest();

        walletRequest.setVendorPlayerUsername(vendorPlayerUsername);
        walletRequest.setVendorBetId(vendorBetId);
        walletRequest.setExternalTransactionId(externalTransactionId);

        return this.idempotentCheck(walletRequest);
    }

    public SportUnsettledBet idempotentCheck(WalletRequest walletRequest) throws BetResultIdempotentViolationException, TransactionStillProcessingException {

        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        String vendorBetId = walletRequest.getVendorBetId();
        String externalTransactionId = walletRequest.getExternalTransactionId();
        SportUnsettledBet sportUnsettledBet = null;

        sportUnsettledBet = sportUnsettledBetRepository.findByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
        if (Objects.isNull(sportUnsettledBet)) {
            sportUnsettledBet = sportUnsettledBetRepository.findById(vendorPlayerUsername + '_' + vendorBetId).orElse(null);
        }

        if (sportUnsettledBet != null) {
            Long betTimingDifferenceInMillieSeconds = this.compareWithExistingTimingDifference(sportUnsettledBet.getVendorBetTime());

            if (sportUnsettledBet.getExternalTransactionId().equals(externalTransactionId)) {
                if (sportUnsettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                    throw new BetResultIdempotentViolationException(sportUnsettledBet);

                } else if (betTimingDifferenceInMillieSeconds < this.getTimingDifferenceForStillProcessing()) {
                    throw new TransactionStillProcessingException("SportUnsettledBetCouchbase idempotentCheck : " + betTimingDifferenceInMillieSeconds + " seconds.");

                } else {
                    //do nothing when externalTransactionId is matched, but status is not OK, we will resend the request to operator
                }
            }
        }

        return sportUnsettledBet;
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

    public List<SportUnsettledBetMariaDB> mariaDBGetByRoundId(String vendorId, String roundId) throws BetNotFoundException {

        return null;
    }

    public SportUnsettledBetMariaDB mariaDBGetByRoundIdAndVendorBetId(Integer vendorId, String roundId, String vendorBetId) throws BetNotFoundException {

        return null;
    }
}
