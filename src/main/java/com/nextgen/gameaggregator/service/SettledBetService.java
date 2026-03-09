package com.nextgen.gameaggregator.service;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.core.error.AmbiguousTimeoutException;
import com.couchbase.client.core.error.UnambiguousTimeoutException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.RawBetIdempotentLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.ga.writer.RawSettledBetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettledBetService {

    private final RawSettledBetRepository rawSettledBetRepository;
    private final BetIdempotentLogService betIdempotentLogService;
    private final BetLookupService betLookupService;

    @Caching(put = {
            @CachePut(value = "SettledBet", key = "{#settledBet.externalTransactionId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager")
    })
    public SettledBet save(SettledBet settledBet, String rawData) {

        if (settledBet.getResettleNum() == null) {
            settledBet.setResettleNum(0);
        }

        if (settledBet.getRawData() == null) {
            if (rawData == null) settledBet.setRawData("");
        }

        settledBet.setProcessingStatus(0);
        rawSettledBetRepository.save(settledBet);

        return settledBet;
    }

    @Caching(put = {
            @CachePut(value = "SettledBet", key = "{#settledBet.externalTransactionId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager")
    })
    public SettledBet update(Integer operatorStatus, BigDecimal balance, SettledBet settledBet) {
        settledBet.setBalance(balance);
        settledBet.setOperatorStatus(operatorStatus);
        settledBet.setId(settledBet.generateId());

        rawSettledBetRepository.save(settledBet);

        return settledBet;
    }

    @Cacheable(value = "SettledBet", key = "{#externalTransactionId, #vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet getByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId) throws BetNotFoundException {
        SettledBet settledBet = rawSettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        if (settledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find vendor player Id: " + vendorPlayerId + ", externalTransactionId: " + externalTransactionId);
        }

        return settledBet;
    }

    //@Retryable(retryFor = {BetNotFoundException.class}, maxAttempts = 3, backoff = @Backoff(delay = 200))
    @Cacheable(value = "SettledBet", key = "{#externalTransactionId, #vendorPlayerId}", cacheManager = "cacheManager", unless = "#result == null")
    public SettledBet getByVendorPlayerIdAndExternalTransactionIdWithRetry(Long vendorPlayerId, String externalTransactionId) throws BetNotFoundException {
        SettledBet settledBet = rawSettledBetRepository.findByVendorPlayerIdAndExternalTransactionIdWithRequestPlus(vendorPlayerId, externalTransactionId);
        if (settledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find vendor player Id: " + vendorPlayerId + ", externalTransactionId: " + externalTransactionId);
        }

        return settledBet;
    }

    public SettledBet get(String id) {
        return rawSettledBetRepository.findById(id).orElse(null);
    }

    public SettledBet getById(String id) throws BetNotFoundException {
        return rawSettledBetRepository.findById(id).orElseThrow(BetNotFoundException::new);
    }

    //Temporary built for ambslot, could be replaced other class file level for unsettledBet endpoint check is bet settled.
    @Cacheable(value = "SettledBet", key = "{#externalTransactionId, #vendorPlayerId}", cacheManager = "cacheManager", unless = "#result == null")
    public SettledBet getById(String id, String externalTransactionId, Long vendorPlayerId) {
        return rawSettledBetRepository.findById(id).orElse(null);
    }

    /**
     * Delete settled bet record of the given settleBet entity object after successful inserted into kafka.
     *
     * @param settledBet entity object containing information of a single settled bet
     */
    public void delete(SettledBet settledBet) {
        try {
            rawSettledBetRepository.delete(settledBet);
        } catch (Exception e) {
            //log.warn("Couchbase Delete SettledBet.exception -> vendorBetId = " + settledBet.getVendorBetId() + "& roundId = " + settledBet.getRoundId());
        }
    }

    public String generateId(BetResultData betResultData, Integer vendorGameId, Long vendorPlayerId) {
        return betResultData.getVendorBetId() + '_' + betResultData.getRoundId() + '_' + vendorGameId.toString() + '_' + vendorPlayerId.toString();
    }

    public SettledBet idempotentCheck(String traceId, GameSession gameSession, BetResultData betResultData)
            throws BetResultIdempotentViolationException, TransactionStillProcessingException,
            AmbiguousTimeoutException, UnambiguousTimeoutException {

        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String vendorBetId = betResultData.getVendorBetId();
        String roundId = betResultData.getRoundId();
        SettledBet settledBet = null;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        try {
            betLookupService.save(vendorBetId, betResultData.getExternalTransactionId(), roundId, vendorGameId, vendorPlayerId);
            String settledBetId = SettledBet.generateId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
            settledBet = this.getById(settledBetId);

            if (settledBet != null) { // duplicate request found in settled_bet
                Integer operatorStatus = settledBet.getOperatorStatus();
                Long betTimingDifferenceInMillieSeconds = betIdempotentLogService.compareWithExistingTimingDifference(settledBet.getCreateTime());
                // throw idempotent exception if status is processing or success
                if (operatorStatus.equals(operatorStatusProcessing) && betTimingDifferenceInMillieSeconds < betIdempotentLogService.getTimingDifferenceForStillProcessing()) {
                    throw new TransactionStillProcessingException();

                } else if (operatorStatus.equals(operatorStatusSuccess)) {
                    throw new BetResultIdempotentViolationException(settledBet);

                } else { // when settled bet found and operator status is error, set status back to processing and resend txn to operator
                    settledBet.setOperatorStatus(operatorStatusProcessing);
                    this.save(settledBet, settledBet.getRawData());
                }
            }
        } catch (BetNotFoundException betNotFoundException) {

            RawBetIdempotentLog betIdempotentLog = null;

            if (betResultData.getVendorBetTime() != null || betResultData.getVendorSettleTime() != null) {
                Long vendorBetTime = (betResultData.getVendorBetTime() != null) ? betResultData.getVendorBetTime() : betResultData.getVendorSettleTime();
                Long timeDifference = System.currentTimeMillis() - vendorBetTime;

                //if vendorBetTime is over 2 hours, check exists against bet_idempotent_log table
                if (timeDifference > betIdempotentLogService.getTimingDifference()) {
                    betIdempotentLog = betIdempotentLogService.checkExists(betResultData, gameSession);

                }

            } else {
                //if vendorBetTime and vendorSettleTime is null, then check against bet_idempotent_log table
                betIdempotentLog = betIdempotentLogService.checkExists(betResultData, gameSession);

            }

            if (betIdempotentLog != null) {

                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonBetResultData = objectMapper.writeValueAsString(betResultData);
                    //log if found matched data after 2 hours for bet idempotent log checking
                    log.info("betIdempotentLogService.checkExists : vendorPlayerUsername = " + gameSession.getVendorPlayerUsername() + ", betResultData = " + jsonBetResultData);

                } catch (JsonProcessingException e) {
                    log.error("generateBetIdempotentId ERROR : " + e.getMessage());

                }

                throw new BetResultIdempotentViolationException(betIdempotentLog);

            }

            // else just process normally as bet not found could be expected
            // save the data into couchbase first for idempotency checks
            SettledBet processingSettledBet = new SettledBet(betResultData, traceId, vendorGameId, vendorPlayerId, gameSession);
            processingSettledBet.setOperatorStatus(operatorStatusProcessing);
            processingSettledBet.setVendorId(gameSession.getVendorId());
            processingSettledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
            this.save(processingSettledBet, "");

        }

        return settledBet;
    }

    public List<SettledBet> getByVendorPlayerIdAndRoundId(Long vendorPlayerId, String roundId) {
        List<SettledBet> settledBetList = rawSettledBetRepository.findByVendorPlayerIdAndRoundId(vendorPlayerId, roundId);

        return settledBetList;
    }

    public List<SettledBet> saveAll(List<SettledBet> settledBetList) {
        rawSettledBetRepository.saveAll(settledBetList);
        return settledBetList;
    }
}
