package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultNotFoundException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BetHistoryService {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private BetHistoryRepository betHistoryRepository;
    @Autowired
    private BetResultLogRepository betResultLogRepository;

    /**
     * Creates a database record of the given BetHistory entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity BetHistory entity object containing information of a single bet
     * @return BetHistory entity object after a successful save
     */
    @CachePut(value = "BetHistories", key = "{#entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory create(BetHistory entity) throws DuplicateExternalTransactionIdException {
        // Set default values
        entity.setWinAmount(BigDecimal.ZERO);
        entity.setWinLoss(BigDecimal.ZERO);
        entity.setVendorWinLoss(BigDecimal.ZERO);
        entity.setEffectiveTurnover(BigDecimal.ZERO);
        entity.setResultType(WinType.LOSE.code);
        entity.setStatus(BetStatus.UNSETTLED.code);
        entity.setCreateTime(System.currentTimeMillis());

        try {
            betHistoryRepository.save(entity);

        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            throw new DuplicateExternalTransactionIdException("Duplicate bet_history " +
                    ", external_transaction_id:" + entity.getExternalTransactionId() +
                    ", round_id:" + entity.getRoundId() +
                    ", vendor_line_id:" + entity.getVendorLineId());
        }

        return entity;
    }

    /**
     * Check for a duplicate vendor transaction Id
     *
     * @param txnId          Vendor's unique Id for each transaction
     * @param gameId         Game Id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @throws DuplicateExternalTransactionIdException If a matching external_transaction_id is found.
     */
    // TODO: performance tuning, read from cache
    public void checkDuplicateExternalTransaction(String txnId, Integer gameId, Long vendorPlayerId) throws DuplicateExternalTransactionIdException {
        BetResultLog resultLog = betResultLogRepository.findByExternalTransactionIdAndVendorGameIdAndVendorPlayerId(txnId, gameId, vendorPlayerId);
        if (resultLog != null) { // Found a matching external transaction Id
            throw new DuplicateExternalTransactionIdException("Duplicate external transaction Id: " + txnId);
        }
    }

    /**
     * Retrieve a bet transaction record based on vendor's round Id
     *
     * @param roundId        Vendor's round Id
     * @param gameId         Game Id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return BetHistory entity object containing all information of a single Bet
     * @throws BetNotFoundException If no bet record is found
     */
    // TODO: performance tuning, read from cache
    @Cacheable(value = "BetHistories", key = "{#roundId, #gameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public BetHistory getBetTransactionByRoundId(String roundId, Integer gameId, Long vendorPlayerId) throws BetNotFoundException {
        BetHistory betHistory = betHistoryRepository.findByRoundIdAndVendorGameIdAndVendorPlayerId(roundId, gameId, vendorPlayerId);
        if (betHistory == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find round Id: " + roundId);
        }
        return betHistory;
    }

    /**
     * Retrieve a bet transaction record based on vendor's unique transaction Id
     *
     * @param externalTransactionId Vendor's unique transaction Id mapped to this field
     * @param vendorId              Vendor's Id with Game Aggregator System
     * @return BetHistory entity object containing all information of a single Bet
     * @throws BetNotFoundException If no bet record is found
     */
    public BetHistory getBetTransactionByVendorTransactionId(String externalTransactionId, Integer vendorId) throws BetNotFoundException {
        BetHistory betHistory = betHistoryRepository.findByExternalTransactionIdAndVendorId(externalTransactionId, vendorId);
        if (betHistory == null) { // No matching bet record for the given transaction Id
            throw new BetNotFoundException("Cannot find external transaction Id: " + externalTransactionId);
        }
        return betHistory;
    }

    public BetHistory getBetTransactionByVendorTransactionIdPlayerId(String externalTransactionId, Integer vendorId, Long vendorPlayerId) throws BetNotFoundException {

        BetHistory betHistory = betHistoryRepository.findByExternalTransactionIdAndVendorIdAndVendorPlayerId(externalTransactionId, vendorId, vendorPlayerId);
        if (betHistory == null) { // No matching bet record for the given transaction Id
            throw new BetNotFoundException("Cannot find external transaction Id: " + externalTransactionId);
        }
        return betHistory;
    }

    public BetResultLog getBetHistoryByExternalTransaction(String txnId, String roundId, Integer vendorLineId) throws BetResultNotFoundException {
        BetResultLog resultLog = betResultLogRepository.findByExternalTransactionIdAndRoundIdAndVendorLineId(txnId, roundId, vendorLineId);
        return resultLog;
    }
}
