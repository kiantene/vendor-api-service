package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.RawSettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class SettledBetService {

    @Autowired
    RawSettledBetRepository rawSettledBetRepository;

    @Caching(put = {
            @CachePut(value = "SettledBet", key = "{#settledBet.externalTransactionId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager"),
            @CachePut(value = "SettledBet", key = "{#settledBet.vendorBetId, #settledBet.roundId, #settledBet.vendorId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager")
    })
    public SettledBet save(SettledBet settledBet, String rawData) {

        if (settledBet.getResettleNum() == null) {
            settledBet.setResettleNum(0);
        }

        if (settledBet.getRawData() == null) {
            if(rawData == null)settledBet.setRawData("");
        }

        settledBet.setProcessingStatus(0);
        rawSettledBetRepository.save(settledBet);

        return settledBet;
    }

    @Caching(put = {
            @CachePut(value = "SettledBet", key = "{#settledBet.externalTransactionId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager"),
            @CachePut(value = "SettledBet", key = "{#settledBet.vendorBetId, #settledBet.roundId, #settledBet.vendorId, #settledBet.vendorPlayerId}", cacheManager = "cacheManager")
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

    @Cacheable(value = "SettledBet", key = "{#vendorBetId, #roundId, #vendorId, #vendorPlayerId}", cacheManager = "cacheManager")
    public SettledBet getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(String vendorBetId, String roundId, Integer vendorId, Long vendorPlayerId) throws BetNotFoundException {

        SettledBet settledBet = rawSettledBetRepository.findByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(vendorBetId, roundId, vendorId, vendorPlayerId);

        if (settledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId");
        }

        return settledBet;
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
            log.warn("Couchbase Delete SettledBet.exception -> vendorBetId = " + settledBet.getVendorBetId() + "& roundId = " + settledBet.getRoundId());
        }
    }

    public String generateId(BetResultData betResultData, Integer vendorGameId, Long vendorPlayerId) {
        return betResultData.getVendorBetId() + '_' + betResultData.getRoundId() + '_' + vendorGameId.toString() + '_' + vendorPlayerId.toString();
    }

    public SettledBet idempotentCheck(String traceId, GameSession gameSession, BetResultData betResultData)
            throws BetResultIdempotentViolationException, TransactionStillProcessingException {

        Integer vendorId = gameSession.getVendorId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        String vendorBetId = betResultData.getVendorBetId();
        String roundId = betResultData.getRoundId();
        SettledBet settledBet = null;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        try {
            settledBet = this.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(vendorBetId, roundId, vendorId, vendorPlayerId);

            if (settledBet != null) { // duplicate request found in settled_bet
                Integer operatorStatus = settledBet.getOperatorStatus();
                // throw idempotent exception if status is processing or success
                if (operatorStatus.equals(operatorStatusProcessing)) {
                    log.warn("idempotentCheck.processing [" + traceId + "]: vendorBetId (" + vendorBetId + ") roundId (" + roundId + ")");
                    throw new TransactionStillProcessingException();

                } else if (operatorStatus.equals(operatorStatusSuccess)) {
                    log.warn("idempotentCheck.success [" + traceId + "]: vendorBetId (" + vendorBetId + ") roundId (" + roundId + ")");
                    throw new BetResultIdempotentViolationException(settledBet);

                } else { // when settled bet found and operator status is error, set status back to processing and resend txn to operator
                    settledBet.setOperatorStatus(operatorStatusProcessing);
                    this.save(settledBet, settledBet.getRawData());
                }
            }
        } catch (BetNotFoundException betNotFoundException) {
            // bet not found is expected
            // save the data into couchbase first for idempotency checks
            SettledBet processingSettledBet = new SettledBet(betResultData, traceId, vendorGameId, vendorPlayerId);
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
}
