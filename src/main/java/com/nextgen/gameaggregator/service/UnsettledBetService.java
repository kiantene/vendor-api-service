package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.RawUnsettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UnsettledBetService {
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    /**
     * Retrieve an unsettled bet transaction record based on vendor's round Id, game Id, and player Id
     *
     * @param roundId        Vendor's round Id
     * @param vendorGameId   vendor game id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return unsettled bet entity object containing all information of a single unsettled Bet
     * @throws BetNotFoundException If no bet record is found
     */
    @Cacheable(value = "UnsettledBet", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet getUnsettledBetByRoundId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId) throws BetNotFoundException {

        String mergeId = vendorBetId + '_' + roundId + '_' + vendorGameId + '_' + vendorPlayerId;
        UnsettledBet unsettledBet = null;

        unsettledBet = rawUnsettledBetRepository.findById(mergeId).orElse(null);
        if (unsettledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find round Id: " + roundId);
        }

        return unsettledBet;
    }

    /**
     * Creates a unsettled bet record of the given RawUnsettledBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity RawUnsettledBet entity object containing information of a single unsettled bet
     * @return RawUnsettledBet entity object after a successful save
     */
    @CachePut(value = "UnsettledBet", key = "{#entity.vendorBetId, #entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet create(UnsettledBet entity) {
        // Set default values
        entity.setCreateTime(System.currentTimeMillis());
        rawUnsettledBetRepository.save(entity);

        return entity;
    }

    @CachePut(value = "UnsettledBet", key = "{#unsettledBet.vendorBetId, #unsettledBet.roundId, #unsettledBet.vendorGameId, #unsettledBet.vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet save(UnsettledBet unsettledBet) {
        rawUnsettledBetRepository.save(unsettledBet);
        return unsettledBet;
    }

    /**
     * Creates a unsettled bet record of the given RawUnsettledBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity RawUnsettledBet entity object containing information of a single unsettled bet
     */
    @CacheEvict(value = "UnsettledBet", key = "{#entity.vendorBetId, #entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public void delete(UnsettledBet entity) {
        rawUnsettledBetRepository.delete(entity);
    }

    public UnsettledBet getByVendorPlayerIdAndExternalTransactionId(Long vendorPlayerId, String externalTransactionId) throws BetNotFoundException {
        UnsettledBet unsettledBet = rawUnsettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, externalTransactionId);
        if (unsettledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find Vendor Player Id: " + vendorPlayerId + ", externalTransactionId: " + externalTransactionId);
        }

        return unsettledBet;
    }

    public UnsettledBet getByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId) throws BetNotFoundException {
        UnsettledBet unsettledBet = rawUnsettledBetRepository.findByVendorIdAndExternalTransactionId(vendorId, externalTransactionId);
        if (unsettledBet == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find Vendor Id: " + vendorId + ", externalTransactionId: " + externalTransactionId);
        }

        return unsettledBet;
    }

    public List<UnsettledBet> getByRoundId(String roundId, Integer vendorGameId, Long vendorPlayerId) {
        return rawUnsettledBetRepository.findByRoundIdAndVendorGameIdAndVendorPlayerId(roundId, vendorGameId, vendorPlayerId);
    }

    public UnsettledBet idempotentCheck(String traceId, GameSession gameSession, BetResultData betResultData)
            throws TransactionStillProcessingException, BetResultIdempotentViolationException {

        String transactionId = betResultData.getExternalTransactionId();
        String roundId = betResultData.getRoundId();
        String vendorBetId = betResultData.getVendorBetId();
        Integer vendorGameId = gameSession.getVendorGameId();
        Long vendorPlayerId = gameSession.getVendorPlayerId();
        UnsettledBet unsettledBet = null;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        try {
            unsettledBet = this.getUnsettledBetByRoundId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
            Integer operatorStatus = unsettledBet.getOperatorStatus();

            // throw idempotent exception if status is processing or success
            if (operatorStatus.equals(operatorStatusProcessing)) {
                log.warn("idempotentCheck.processing [" + traceId + "]: transactionId (" + transactionId + ") roundId (" + roundId + ")");
                throw new TransactionStillProcessingException();

            } else if (operatorStatus.equals(operatorStatusSuccess)) {
                log.warn("idempotentCheck.success [" + traceId + "]: transactionId (" + transactionId + ") roundId (" + roundId + ")");
                throw new BetResultIdempotentViolationException(unsettledBet);

            } else { // when bet result found and operator status is error, set status back to processing and resend txn to operator
                unsettledBet.setOperatorStatus(operatorStatusProcessing);
                this.save(unsettledBet);
            }
        } catch (BetNotFoundException betNotFoundException) {
            UnsettledBet newUnsettledBet = this.newUnsettledBet(gameSession, "", betResultData, traceId, ResultType.BET.code);
            this.create(newUnsettledBet);
        }

        return unsettledBet;
    }

    public UnsettledBet newUnsettledBet(GameSession gameSession, String rawData,
                                         BetResultData betResultData, String traceId, Integer resultType) {

        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        UnsettledBet unsettledBet = new UnsettledBet(betResultData);

        unsettledBet.setInternalTransactionId(traceId);
        unsettledBet.setBetId(traceId);
        unsettledBet.setVendorGameId(gameSession.getVendorGameId());
        unsettledBet.setVendorPlayerId(gameSession.getVendorPlayerId());
        unsettledBet.setVendorId(gameSession.getVendorId());
        unsettledBet.setAgentPlayerId(gameSession.getAgentPlayerId());
        unsettledBet.setAgentId(gameSession.getAgentId());
        unsettledBet.setVendorLineId(gameSession.getVendorLineId());
        unsettledBet.setGameCategoryId(gameSession.getGameCategoryId());
        unsettledBet.setCurrencyId(gameSession.getCurrencyId());
        unsettledBet.setGameSessionToken(gameSession.getToken());
        unsettledBet.setResultType(resultType);
        unsettledBet.setGameSessionToken(gameSession.getToken());
        unsettledBet.setOperatorStatus(operatorStatusProcessing);
        unsettledBet.setRawData(rawData);
        unsettledBet.setIsFreespin(Optional.ofNullable(betResultData.getIsFreespin()).orElse(0));
        unsettledBet.setStatus(betResultData.getBetStatus().code);

        return unsettledBet;
    }
}
