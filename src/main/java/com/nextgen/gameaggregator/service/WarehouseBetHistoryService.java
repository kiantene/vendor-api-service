package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.entity.warehouse.BetDetailUrlInfo;
import com.nextgen.gameaggregator.entity.warehouse.BetHistory;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.list.TransactionsListDto;
import com.nextgen.gameaggregator.repository.ga.writer.VendorCurrencyRepository;
import com.nextgen.sas.core.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class WarehouseBetHistoryService {
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;
    private final GameCategoryService gameCategoryService;
    private final CurrencyService currencyService;
    private final Integer[] excludeGameCategoryIds = {6}; // skip sport category
    private final NamedParameterJdbcTemplate clickHouseJdbcTemplate;
    private final VendorCurrencyRepository vendorCurrencyRepository;

    private final S3BetService s3BetService;
    @Autowired
    public WarehouseBetHistoryService (VendorGameService vendorGameService, VendorService vendorService,
                                       GameCategoryService gameCategoryService, CurrencyService currencyService,
                                       NamedParameterJdbcTemplate clickHouseJdbcTemplate,
                                       VendorCurrencyRepository vendorCurrencyRepository,
                                       S3BetService s3BetService){
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
        this.gameCategoryService = gameCategoryService;
        this.currencyService = currencyService;
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
        this.vendorCurrencyRepository = vendorCurrencyRepository;
        this.s3BetService = s3BetService;

    }

    public List<Object> findByAgentIdAndSettledTimeBetween(Integer agentId, TransactionsListDto dto) throws SQLException {

        //note: Convert all the time to UTC for partition because partition in the DB is UTC
        String dateFormat = "yyyyMMdd";
        String startDateStr = DateUtil.convertMiliToDateString(dto.getFromTime(), "UTC", dateFormat);
        String endDateStr = DateUtil.convertMiliToDateString(dto.getToTime(), "UTC", dateFormat);

        String sqlStmt =
                "SELECT " +
                        "id, round_id, external_transaction_id, agent_player_username, currency_code, game_code, vendor_code, " +
                        "game_category_code, bet_amount, win_amount, win_loss, effective_turnover, jackpot_amount, status, vendor_bet_time, " +
                        "vendor_settle_time,  IF(is_freespin =0 ,'FALSE','TRUE') AS isFreeSpin, vendor_bet_id " +
                        "FROM bet_history WHERE toYYYYMMDD(toDateTime(vendor_settle_time/1000)) BETWEEN :startDateStr AND :endDateStr " +
                        " AND agent_id = :agentId AND vendor_settle_time BETWEEN :startTime AND :endTime " +
                        " ORDER BY vendor_settle_time, id, resettle_num ASC LIMIT :limit OFFSET :offset";

        Map<String, Object> params = new HashMap<>();
        params.put("agentId", agentId);
        params.put("startDateStr", startDateStr);
        params.put("endDateStr", endDateStr);
        params.put("startTime", dto.getFromTime());
        params.put("endTime", dto.getToTime());
        params.put("offset", ((dto.getPageNo() - 1) * dto.getPageSize()));
        params.put("limit", dto.getPageSize());

        List<Object> transactions = new ArrayList<>();

        clickHouseJdbcTemplate.query(sqlStmt, params, rs -> {

            while (rs.next()) {
                ArrayList<Object> bet = new ArrayList<>();
                bet.add(rs.getString("id"));
                bet.add(rs.getString("round_id"));
                bet.add(rs.getString("external_transaction_id"));
                bet.add(rs.getString("agent_player_username"));
                bet.add(rs.getString("currency_code"));
                bet.add(rs.getString("game_code"));
                bet.add(rs.getString("vendor_code"));
                bet.add(rs.getString("game_category_code"));
                bet.add(rs.getBigDecimal("bet_amount"));
                bet.add(rs.getBigDecimal("win_amount"));
                bet.add(rs.getBigDecimal("win_loss"));
                bet.add(rs.getBigDecimal("effective_turnover"));
                bet.add(rs.getBigDecimal("jackpot_amount"));
                bet.add(rs.getInt("status"));
                bet.add(rs.getLong("vendor_bet_time"));
                bet.add(rs.getLong("vendor_settle_time"));
                bet.add(rs.getString("isFreeSpin"));
                bet.add(rs.getString("vendor_bet_id"));
                transactions.add(bet);
            }
            return transactions; // adapt as necessary

        });

        return transactions;
    }

    public Long findByAgentIdAndCreateTimeBetweenCount(Integer agentId, TransactionsListDto dto) throws SQLException {
        //note: Convert all the time to UTC for partition because partition in the DB is UTC
        String dateFormat = "yyyyMMdd";
        String startDateStr = DateUtil.convertMiliToDateString(dto.getFromTime(), "UTC", dateFormat);
        String endDateStr = DateUtil.convertMiliToDateString(dto.getToTime(), "UTC", dateFormat);


        String sqlStmt =
                "SELECT COUNT(1)" +
                        "FROM bet_history WHERE toYYYYMMDD(toDateTime(vendor_settle_time/1000)) BETWEEN :startDateStr AND :endDateStr " +
                        " AND agent_id = :agentId AND vendor_settle_time BETWEEN :startTime AND :endTime ";
        
        Map<String, Object> params = new HashMap<>();
        params.put("agentId", agentId);
        params.put("startDateStr", startDateStr);
        params.put("endDateStr", endDateStr);
        params.put("startTime", dto.getFromTime());
        params.put("endTime", dto.getToTime());

        long startTime = System.currentTimeMillis();
        Long totalRecord = clickHouseJdbcTemplate.queryForObject(sqlStmt, params, Long.class);
        long duration = System.currentTimeMillis() - startTime;

        if (totalRecord != null && (totalRecord % 2000 == 0)) {
            log.info("CHECK CLICKHOUSE QUERY - SQL: {} - Params: {} - Total Records: {} - Duration: {} ms",
                    sqlStmt, params, totalRecord, duration);
        }
        return totalRecord;
    }

    public IBetDetailUrlInfo getBetHistoryDetail(Integer agentId, String betId) throws BetNotFoundException {
        String sqlStmt =
                "SELECT " +
                        "id , round_id, external_transaction_id, " +
                        "agent_player_username, currency_id,  currency_code, " +
                        "vendor_player_username, game_code, vendor_id,  vendor_code, " +
                        "game_category_code, bet_amount, win_amount, " +
                        "win_loss, effective_turnover, jackpot_amount, " +
                        "status, vendor_bet_time, " +
                        "vendor_settle_time, vendor_line_id, " +
                        "IF(is_freespin =0 ,'FALSE','TRUE') AS isFreeSpin, " +
                        "game_session_token " +
                        "FROM bet_history WHERE " +
                        "agent_id = :agentId AND id= :betId " +
                        "ORDER BY id, vendor_bet_time ASC " +
                        "LIMIT 1 ";

        Map<String, Object> params = new HashMap<>();
        params.put("agentId", agentId);
        params.put("betId", betId);


        BetDetailUrlInfo betDetailUrlInfo =  new BetDetailUrlInfo();

        clickHouseJdbcTemplate.query(sqlStmt, params, rs -> {
            while (rs.next()) {
                betDetailUrlInfo.setBetId(rs.getString("id"));
                betDetailUrlInfo.setExternalTransactionId(rs.getString("external_transaction_id"));
                betDetailUrlInfo.setExternalRoundId(rs.getString("round_id"));
                betDetailUrlInfo.setUsername(rs.getString("agent_player_username"));
                betDetailUrlInfo.setCurrencyId(rs.getInt("currency_id"));
                betDetailUrlInfo.setCurrencyCode(rs.getString("currency_code"));
                betDetailUrlInfo.setGameCode(rs.getString("game_code"));
                betDetailUrlInfo.setVendorId(rs.getInt("vendor_id"));
                betDetailUrlInfo.setVendorCode(rs.getString("vendor_code"));
                betDetailUrlInfo.setGameCategoryCode(rs.getString("game_category_code"));
                betDetailUrlInfo.setBetAmount(rs.getBigDecimal("bet_amount"));
                betDetailUrlInfo.setWinAmount(rs.getBigDecimal("win_amount"));
                betDetailUrlInfo.setWinLoss(rs.getBigDecimal("win_loss"));
                betDetailUrlInfo.setEffectiveTurnover(rs.getBigDecimal("effective_turnover"));
                betDetailUrlInfo.setJackpotAmount(rs.getBigDecimal("jackpot_amount"));
                betDetailUrlInfo.setStatus(rs.getInt("status"));
                betDetailUrlInfo.setVendorBetTime(rs.getLong("vendor_bet_time"));
                betDetailUrlInfo.setVendorSettleTime(rs.getLong("vendor_settle_time"));
                betDetailUrlInfo.setVendorLineId(rs.getInt("vendor_line_id"));
                betDetailUrlInfo.setIsFreeSpin(rs.getString("isFreeSpin"));
                betDetailUrlInfo.setVendorUsername(rs.getString("vendor_player_username"));
                betDetailUrlInfo.setGameSessionToken(rs.getString("game_session_token"));

                VendorCurrency vendorCurrency =
                        vendorCurrencyRepository.findByVendorIdAndCurrencyId
                                (rs.getInt("vendor_id"), rs.getInt("currency_id"));
                betDetailUrlInfo.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());

            }
            return betDetailUrlInfo; // adapt as necessary

        });
        return (betDetailUrlInfo.getBetId() == null) ? null : betDetailUrlInfo;

    }


    public IBetDetailUrlInfo getBetHistoryDetailFromS3(String betId) throws BetNotFoundException {
        BetDetailUrlInfo betDetailUrlInfo =  new BetDetailUrlInfo();

        try {
            com.nextgen.gameaggregator.entity.warehouse.BetHistory warehouseBetHistory = s3BetService.readBetHistoryFromS3File(betId);


            betDetailUrlInfo.setBetId(warehouseBetHistory.getId());
            betDetailUrlInfo.setExternalTransactionId(warehouseBetHistory.getExternalTransactionId());
            betDetailUrlInfo.setExternalRoundId(warehouseBetHistory.getRoundId());
            betDetailUrlInfo.setUsername(warehouseBetHistory.getAgentPlayerUsername());
            betDetailUrlInfo.setCurrencyId(warehouseBetHistory.getCurrencyId());
            betDetailUrlInfo.setCurrencyCode(warehouseBetHistory.getCurrencyCode());
            betDetailUrlInfo.setGameCode(warehouseBetHistory.getGameCode());
            betDetailUrlInfo.setVendorId(warehouseBetHistory.getVendorId());
            betDetailUrlInfo.setVendorCode(warehouseBetHistory.getVendorCode());
            betDetailUrlInfo.setGameCategoryCode(warehouseBetHistory.getGameCategoryCode());
            betDetailUrlInfo.setBetAmount(warehouseBetHistory.getBetAmount());
            betDetailUrlInfo.setWinAmount(warehouseBetHistory.getWinAmount());
            betDetailUrlInfo.setWinLoss(warehouseBetHistory.getWinLoss());
            betDetailUrlInfo.setEffectiveTurnover(warehouseBetHistory.getEffectiveTurnover());
            betDetailUrlInfo.setJackpotAmount(warehouseBetHistory.getJackpotAmount());
            betDetailUrlInfo.setStatus(warehouseBetHistory.getStatus());
            betDetailUrlInfo.setVendorBetTime(warehouseBetHistory.getVendorBetTime());
            betDetailUrlInfo.setVendorSettleTime(warehouseBetHistory.getVendorSettleTime());
            betDetailUrlInfo.setVendorLineId(warehouseBetHistory.getVendorLineId());
            betDetailUrlInfo.setIsFreeSpin(warehouseBetHistory.getIsFreespin() == 0 ? "FALSE" : "TRUE");
            betDetailUrlInfo.setVendorUsername(warehouseBetHistory.getVendorPlayerUsername());
            betDetailUrlInfo.setGameSessionToken(warehouseBetHistory.getGameSessionToken());

            VendorCurrency vendorCurrency =
                    vendorCurrencyRepository.findByVendorIdAndCurrencyId
                            (warehouseBetHistory.getVendorId(), warehouseBetHistory.getCurrencyId());
            betDetailUrlInfo.setVendorCurrencyCode(vendorCurrency.getVendorCurrencyCode());
        }catch ( Exception exception){
            log.error("Error reading S3 file :" + exception.getMessage());
        }
        return (betDetailUrlInfo.getBetId() == null) ? null : betDetailUrlInfo;

    }

    public void setWarehouseBetHistoryInfoCache(VendorGame vendorGame, Currency currency) {

        CompletableFuture<VendorGame> futureVendorGame = CompletableFuture.supplyAsync(() -> {
            try {
                return vendorGameService.getByGameId(vendorGame.getId(), vendorGame);
            } catch (GameNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Currency> futureCurrency = CompletableFuture.supplyAsync(() -> {
            try {
                return currencyService.getByCurrencyId(currency.getId(), currency);
            } catch (InvalidCurrencyException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf
                (futureVendorGame, futureCurrency).join(); // Wait for all to complete
    }

    public WarehouseFutureEntity getWarehouseBetHistoryInfoCache(Integer vendorGameId, Integer vendorId, Integer gameCategoryId, Integer currencyId) {

        CompletableFuture<VendorGame> futureVendorGame = CompletableFuture.supplyAsync(() -> {
            try {
                return vendorGameService.getByGameId(vendorGameId, null);
            } catch (GameNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Vendor> futureVendor = CompletableFuture.supplyAsync(() -> {
            try {
                return vendorService.getByVendorId(vendorId, null);
            } catch (InvalidVendorException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Currency> futureCurrency = CompletableFuture.supplyAsync(() -> {
            try {
                return currencyService.getByCurrencyId(currencyId, null);
            } catch (InvalidCurrencyException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<GameCategory> futureGameCategory = CompletableFuture.supplyAsync(() -> {
            try {
                return gameCategoryService.getByGameCategoryId(gameCategoryId, null);
            } catch (InvalidGameCategoryException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(futureVendorGame, futureVendor, futureCurrency, futureGameCategory).join(); // Wait for all to complete

        WarehouseFutureEntity warehouseFutureEntity = new WarehouseFutureEntity();

        try {

            warehouseFutureEntity.setVendorGame(futureVendorGame.get());
            warehouseFutureEntity.setVendor(futureVendor.get());
            warehouseFutureEntity.setGameCategory(futureGameCategory.get());
            warehouseFutureEntity.setCurrency(futureCurrency.get());

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return warehouseFutureEntity;
    }


    public Boolean checkIsDelaySettlement( BetHistory warehouseBetHistory ){

        boolean isDelaySettlement = false;
        if(Arrays.stream(excludeGameCategoryIds).noneMatch(n -> Objects.equals(n, warehouseBetHistory.getGameCategoryId()))){
            Instant daysAgo = Instant.now().minus(Duration.ofDays(5)); // Subtract 5 days from the current time
            if(warehouseBetHistory.getVendorBetTime()< daysAgo.toEpochMilli()){
                isDelaySettlement = true;
            }
        }

        return isDelaySettlement;
    }
}
