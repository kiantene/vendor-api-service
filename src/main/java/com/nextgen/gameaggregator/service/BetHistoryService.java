package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.data.mariadb.config.MariaDefaultDataSourceConfig;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo;
import com.nextgen.gameaggregator.operator.transactions.detail.TransactionDetailData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.RawUnsettledBetRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BetHistoryService {
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    @Autowired
    private MariaDefaultDataSourceConfig mariaDefaultDataSourceConfig;

    @Autowired
    private VendorLineService vendorLineService;

    public Long getVendorSettleTime(BetResultData betResultData, UnsettledBet unsettledBet) {
        long settledTime = System.currentTimeMillis();

        if (betResultData.getVendorSettleTime() != null) {
            settledTime = betResultData.getVendorSettleTime();
        } else if (unsettledBet != null && unsettledBet.getVendorSettleTime() != null) {
            settledTime = unsettledBet.getVendorSettleTime();
        }

        return settledTime;
    }

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
                entity.getEffectiveTurnover(), entity.getResultType(), entity.getStatus(),
                entity.getVendorBetTime(), entity.getVendorSettleTime(), entity.getResultTime());

        return entity;
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

    public IBetDetailUrlInfo getBetHistoryDetail(Integer agentId, String betId) throws BetNotFoundException {
        IBetDetailUrlInfo iBetDetailUrlInfo = betHistoryRepository.findByIdAndAgentId(agentId, betId);

        if (iBetDetailUrlInfo == null) { // No matching bet record for the given transaction Id
            throw new BetNotFoundException();
        }
        return iBetDetailUrlInfo;
    }

    public TransactionDetailData getDetailUrl(IBetDetailUrlInfo iBetDetailUrlInfo, TransactionDetailData transactionDetailData,
                                              VendorLine vendorLine, VendorLanguageCode vendorLanguageCode) throws
            InvalidVendorResponseException, DisabledVendorLineException, InvalidVendorLineException {


        //2. get vendor line credential
        Map<String, String> credentials = vendorLineService.toCredentialMap(vendorLine);


        try {
            String className = "com.nextgen.gameaggregator.vendor." + vendorLine.getVendor().getClassName() + ".api.betdetail.BetDetailService";
            BetDetailUrl betDetailUrl = (BetDetailUrl) Class.forName(className).getConstructor().newInstance();
            autowireCapableBeanFactory.autowireBean(betDetailUrl);
            MultiValueMap<String, String> formData = betDetailUrl.formDataBuilder(credentials, iBetDetailUrlInfo, vendorLanguageCode);

            BetDetailUrlVo betDetailUrlVo = betDetailUrl.call(formData, credentials, iBetDetailUrlInfo, vendorLanguageCode);
            transactionDetailData.setDetailUrl(betDetailUrlVo.getBetDetailUrl());

            return transactionDetailData;
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | InvalidVendorLineException |
                 InvalidFormatException | RecordNotFoundException
                gameClassException) {
            gameClassException.printStackTrace();
            log.error("GAME CLASS ERROR :"+gameClassException.getStackTrace().toString());
            throw new InvalidVendorResponseException();
        }
    }
}
