package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.data.mariadb.config.MariaDefaultDataSourceConfig;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultNotFoundException;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.repository.RawResultBetRepository;
import com.nextgen.gameaggregator.repository.RawUnsettledBetRepository;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class BetHistoryService {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private BetHistoryRepository betHistoryRepository;
    @Autowired
    private BetResultLogRepository betResultLogRepository;

    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    @Autowired
    private MariaDefaultDataSourceConfig mariaDefaultDataSourceConfig;

    @Autowired
    private RawResultBetRepository rawResultBetRepository;

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
        entity.setEffectiveTurnover(BigDecimal.ZERO);
        entity.setResultType(ResultType.LOSE.code);
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
        //JPA INSERT
        //betHistoryRepository.save(entity);

        //JDBC INSERT
//        this.jdbcCreate(entity);

        //COUCHBASE INSERT
//        this.couchbaseCreate(entity);

        return entity;
    }

    /**
     * Creates a unsettled bet record of the given RawUnsettledBet entity object.
     * This function will also populate default values of certain fields.
     *
     * @param entity RawUnsettledBet entity object containing information of a single unsettled bet
     * @return RawUnsettledBet entity object after a successful save
     */
    @CachePut(value = "UnsettledBet", key = "{#entity.vendorBetId, #entity.roundId, #entity.vendorGameId, #entity.vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet createUnsettledBet(UnsettledBet entity) throws CouchbaseDataIntegrityException {
        // Set default values
        entity.setCreateTime(System.currentTimeMillis());
//        entity.setResettleNum(0);

        try {
            rawUnsettledBetRepository.save(entity);

        } catch (DataIntegrityViolationException dataIntegrityViolationException) {

            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
        }

        return entity;
    }

    @Transactional
    public BetHistory jdbcCreate(BetHistory entity) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(mariaDefaultDataSourceConfig.mariaDataSource());

        // Set default values
        entity.setWinAmount(BigDecimal.ZERO);
        entity.setWinLoss(BigDecimal.ZERO);
        //entity.setVendorWinLoss(BigDecimal.ZERO);
        entity.setEffectiveTurnover(BigDecimal.ZERO);
        entity.setResultType(ResultType.LOSE.code);
        entity.setStatus(BetStatus.UNSETTLED.code);
        entity.setCreateTime(System.currentTimeMillis());

        jdbcTemplate.update("INSERT INTO bet_history (id, external_transaction_id, round_id, vendor_game_id, " +
                "vendor_player_id, vendor_id, vendor_line_id, agent_player_id, agent_id, operator_status, " +
                "game_session_token, master_agent_id, house_id, game_category_id, currency_id, bet_amount, " +
                "win_amount, win_loss, vendor_win_loss, effective_turnover, result_type, raw_data, status, " +
                "vendor_bet_time, vendor_settle_time, create_time, result_time) VALUES (?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", entity.getId(), entity.getExternalTransactionId(),
                entity.getRoundId(), entity.getVendorGameId(), entity.getVendorPlayerId(), entity.getVendorId(),
                entity.getVendorLineId(), entity.getAgentPlayerId(), entity.getAgentId(), entity.getOperatorStatus(),
                entity.getGameSessionToken(), entity.getGameCategoryId(),
                entity.getCurrencyId(), entity.getBetAmount(), entity.getWinAmount(), entity.getWinLoss(),
                entity.getEffectiveTurnover(), entity.getResultType(), entity.getRawData(), entity.getStatus(),
                entity.getVendorBetTime(), entity.getVendorSettleTime(), entity.getCreateTime(), entity.getResultTime());

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
     * Retrieve an unsettled bet transaction record based on vendor's round Id, game Id, and player Id
     *
     * @param roundId        Vendor's round Id
     * @param vendorLineId         vendor line id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return unsettled bet entity object containing all information of a single unsettled Bet
     * @throws BetNotFoundException If no bet record is found
     */
    @Cacheable(value = "UnsettledBet", key = "{#vendorBetId, #roundId, #vendorLineId, #vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet getUnsettledBetByRoundId(String vendorBetId, String roundId, Integer vendorLineId, Long vendorPlayerId) throws BetNotFoundException, CouchbaseDataIntegrityException {

        String mergeId = vendorBetId+'_'+roundId+'_'+vendorLineId+'_'+vendorPlayerId;
        UnsettledBet unsettledBet = null;

        try{
             unsettledBet = rawUnsettledBetRepository.findById(mergeId).orElse(null);
            if (unsettledBet == null) { // No matching bet record for the given round Id
                throw new BetNotFoundException("Cannot find round Id: " + roundId);
            }
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
        }

        return unsettledBet;
    }

    /**
     * Retrieve all bets within the same rounds and process together to kafka
     *
     * @param roundId        Vendor's round Id
     * @param vendorLineId         vendor line id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return A list of unsettled bet entity object containing all information
     * @throws To do if connection failed when accessing to couchbase
     */
    public List<UnsettledBet> getBetDataListByRoundId(String roundId, Integer vendorLineId, Long vendorPlayerId){

        try{
            List<UnsettledBet> unsettledBetLists = rawUnsettledBetRepository.findByRoundId(roundId, vendorLineId, vendorPlayerId);

            if (unsettledBetLists == null) {
                return null;
            }

            return unsettledBetLists;

        } catch (Exception e) {
            //TODO ERROR HANDLING IF CONNECTION TO COUCHBASE IS FAILED
            log.error("getBetDataListByRoundId ERROR, details : " + e);
            return null;
        }
    }

    /**
     * Retrieve an unsettled bet transaction record based on vendor's round Id, game Id, and player Id
     *
     * @param roundId        Vendor's round Id
     * @param vendorGameId         vendor game id within Game Aggregator System
     * @param vendorPlayerId Id of the record in VendorPlayer
     * @return unsettled bet entity object containing all information of a single unsettled Bet
     * @throws BetNotFoundException If no bet record is found
     */
    @Cacheable(value = "UnsettledBetWithGameId", key = "{#vendorBetId, #roundId, #vendorGameId, #vendorPlayerId}", cacheManager = "cacheManager")
    public UnsettledBet getRawUnsettledBetByBetIdAndRoundIdAndGameIdAndPlayerId(String vendorBetId, String roundId, Integer vendorGameId, Long vendorPlayerId) throws BetNotFoundException, CouchbaseDataIntegrityException {

        UnsettledBet unsettledBet = null;

        try{
            unsettledBet = rawUnsettledBetRepository.findByVendorBetIdAndRoundIdAndVendorGameIdAndVendorPlayerId(vendorBetId, roundId, vendorGameId, vendorPlayerId);
            if (unsettledBet == null) { // No matching bet record for the given round Id
                throw new BetNotFoundException("Cannot find round Id: " + roundId);
            }
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw new CouchbaseDataIntegrityException("Data incorrect : " + dataIntegrityViolationException.getMessage());
        }

        return unsettledBet;
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
