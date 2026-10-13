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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class SportUnsettledBetService {

    /** Poll interval while waiting for an in-flight placebet to reach a terminal operator outcome. */
    private static final long IN_FLIGHT_POLL_INTERVAL_MILLIS = 100L;

    /**
     * Upper bound on that wait. Must exceed a placebet's own worst case or the wait expires before the
     * debit resolves and stops protecting anything: the operator call is capped at
     * EndPoints.SPORTBOOK_TIMEOUT (5s — the reactor timeout sits downstream of the retry, so it bounds the
     * whole retry chain) and ONEAPI-974 observed 6489ms end to end, so 8s leaves margin.
     *
     * <p>The wait runs on SportWalletService.THREAD_POOL (32 threads), never on a request thread. The
     * write-ahead itself keeps the cost down: almost every placebet now leaves a document, so the "bet is
     * simply absent" case that waits out the full bound is rarer than it was.
     *
     * <p>Expressed as time but spent as attempts: awaitPlaceBetOutcome divides this by
     * IN_FLIGHT_POLL_INTERVAL_MILLIS to get a fixed attempt count, so the two constants move together.
     */
    private static final long IN_FLIGHT_MAX_WAIT_MILLIS = 8_000L;

    private final SportUnsettledBetRepository sportUnsettledBetRepository;
    private final long inFlightMaxWaitMillis;

    @Autowired
    public SportUnsettledBetService(SportUnsettledBetRepository sportUnsettledBetRepository) {
        this(sportUnsettledBetRepository, IN_FLIGHT_MAX_WAIT_MILLIS);
    }

    /** Visible for testing, so the wait bound does not have to be slept through in full. */
    SportUnsettledBetService(SportUnsettledBetRepository sportUnsettledBetRepository, long inFlightMaxWaitMillis) {
        this.sportUnsettledBetRepository = sportUnsettledBetRepository;
        this.inFlightMaxWaitMillis = inFlightMaxWaitMillis;
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

    @Recover
    public SportUnsettledBet recoverData(Exception ex) throws Exception {
        // Other exception in retryable function will just throw out
        throw ex;
    }

    /**
     * The shared bet lookup — used by settle, confirm and the Pinnacle paths.
     *
     * <p>ONEAPI-974: a write-ahead record whose operator debit has not been attempted yet is NOT a usable
     * bet, and is reported as not-found here. Without that, every caller of this method would start seeing
     * a state none of them were written for: they all predate the write-ahead, when "a record exists" could
     * only mean "the operator call finished". A settle landing in that window would treat the record as
     * complete and pay out a win on a stake that may never be debited.
     *
     * <p>Reporting it as not-found keeps all of them behaving exactly as they did before the write-ahead
     * existed. The only reader built for the in-flight state is SportRefundProcessor, which goes through
     * {@link #awaitPlaceBetOutcome} instead and deliberately waits for the outcome.
     *
     * <p>The {@code @Retryable} above also gives roughly 300ms of grace, so a placebet that finishes
     * quickly is picked up on a retry rather than rejected.
     */
    @Retryable(retryFor = {BetNotFoundException.class}, maxAttempts = 6, backoff = @Backoff(delay = 50))
    public SportUnsettledBet getByVendorPlayerUsernameAndVendorBetId(String vendorPlayerUsername, String vendorBetId) throws BetNotFoundException {
        SportUnsettledBet sportUnsettledBet = this.fetch(vendorPlayerUsername, vendorBetId);

        if (sportUnsettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find Id from SportUnsettledBetCouchbase getByVendorPlayerUsernameAndVendorBetId: " + vendorPlayerUsername + ", " + vendorBetId);
        }

        if (isPlaceBetInFlight(sportUnsettledBet)) { // Write-ahead record, operator debit not attempted yet
            throw new BetNotFoundException("Placebet still in flight, not a usable bet yet: " + vendorPlayerUsername + ", " + vendorBetId);
        }

        return sportUnsettledBet;
    }

    /**
     * Bet lookup for the refund / cancel path.
     *
     * <p>ONEAPI-974: SportSingleBetProcessor writes the bet ahead of the operator debit, so a cancel that
     * races an in-flight placebet can now find a bet whose debit has not been attempted yet. Refunding in
     * that window risks the refund reaching the operator BEFORE the debit does, which would leave the
     * player debited with the refund silently rejected. So wait, bounded, for the placebet to finish.
     *
     * <p>If the bet is still in flight when the wait expires we return it anyway and let the refund run:
     * the debit outcome is then indeterminate, and an indeterminate debit must still be refunded — the
     * operator may well have committed it.
     *
     * <p>An absent bet comes back empty rather than throwing. The caller must decide, because a
     * successful refund DELETES the unsettled bet: treating absence as not-found before consulting
     * sport_settled_bet would answer a legitimate duplicate cancel with BetNotFoundException instead of
     * BetResultIdempotentViolationException.
     */
    public Optional<SportUnsettledBet> awaitPlaceBetOutcome(String vendorPlayerUsername, String vendorBetId) {
        SportUnsettledBet sportUnsettledBet = this.fetch(vendorPlayerUsername, vendorBetId);

        // Why there is a loop here at all: the placebet's outcome is written by a DIFFERENT thread (the
        // one serving /placebet), and Couchbase gives us no way to be notified when that write lands. The
        // only way to observe it is to look again. So this polls.
        //
        // Why a counted for-loop rather than "while (not done and before deadline)": the bound is then
        // structural. maxAttempts is fixed before the first iteration and the counter only ever increases,
        // so the loop cannot be made to run forever by a later edit to the exit condition — the worst a
        // mistake can do is poll the wrong number of times. A deadline-based while-loop gives no such
        // guarantee: it also depends on System.currentTimeMillis(), which NTP can step backwards, so a
        // clock adjustment mid-wait could stretch the wait well past its intended bound.
        //
        // Cost of the bound: maxAttempts * IN_FLIGHT_POLL_INTERVAL_MILLIS of wall time and that many KV
        // reads, on a SportWalletService.THREAD_POOL thread (32 of them). At the defaults that is 80
        // attempts over 8s. Changing either constant changes both, which is the intent.
        final int maxAttempts = (int) (this.inFlightMaxWaitMillis / IN_FLIGHT_POLL_INTERVAL_MILLIS);

        for (int attempt = 0;
             attempt < maxAttempts && (sportUnsettledBet == null || isPlaceBetInFlight(sportUnsettledBet));
             attempt++) {
            try {
                Thread.sleep(IN_FLIGHT_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
            sportUnsettledBet = this.fetch(vendorPlayerUsername, vendorBetId);
        }

        if (sportUnsettledBet != null && isPlaceBetInFlight(sportUnsettledBet)) {
            log.warn("Placebet still in flight after {}ms - refunding anyway, operator debit outcome is indeterminate [vendorPlayerUsername={}, vendorBetId={}]",
                    this.inFlightMaxWaitMillis, vendorPlayerUsername, vendorBetId);
        }

        return Optional.ofNullable(sportUnsettledBet);
    }

    /**
     * True while the placebet has written its intent but has no terminal operator outcome yet.
     * Note that {@code status} cannot be used for this: BetStatus.UNSETTLED and
     * SC_TRANSACTION_STILL_PROCESSING are both 0, and a failed placebet also leaves status at 0.
     */
    private boolean isPlaceBetInFlight(SportUnsettledBet sportUnsettledBet) {
        return Objects.equals(sportUnsettledBet.getOperatorStatus(),
                ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code);
    }

    private SportUnsettledBet fetch(String vendorPlayerUsername, String vendorBetId) {
        // KV get by document key (== generateId()) first: hits the data nodes directly and is immune to
        // query-service rescaling / config churn that intermittently parks N1QL with BUCKET_OPEN_IN_PROGRESS.
        SportUnsettledBet sportUnsettledBet = sportUnsettledBetRepository
                .findById(vendorPlayerUsername + '_' + vendorBetId).orElse(null);

        // Fallback to N1QL only on a KV miss (e.g. a legacy doc stored under a non-standard key).
        if (sportUnsettledBet == null) {
            sportUnsettledBet = sportUnsettledBetRepository.findByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
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
            // ONEAPI-974: vendorBetTime is not populated by every vendor (Marblex sets none, Pinnacle parses
            // one that can be null), and unboxing it unconditionally here was a latent NPE. Only the timing
            // branch needs it now — an in-flight placebet is recognised by its write-ahead marker instead.
            Long vendorBetTime = sportUnsettledBet.getVendorBetTime();
            Long betTimingDifferenceInMillieSeconds = (vendorBetTime == null)
                    ? null
                    : this.compareWithExistingTimingDifference(vendorBetTime);

            if (Objects.equals(sportUnsettledBet.getExternalTransactionId(), externalTransactionId)) {
                if (Objects.equals(sportUnsettledBet.getStatus(), ResponseCodes.Status.SC_OK.code)) {
                    throw new BetResultIdempotentViolationException(sportUnsettledBet);

                } else if (isPlaceBetInFlight(sportUnsettledBet)) {
                    throw new TransactionStillProcessingException("SportUnsettledBetCouchbase idempotentCheck : placebet still in flight.");

                } else if (betTimingDifferenceInMillieSeconds != null
                        && betTimingDifferenceInMillieSeconds < this.getTimingDifferenceForStillProcessing()) {
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
