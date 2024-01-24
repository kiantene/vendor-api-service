package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BetHistoryRepository extends JpaRepository<BetHistory, String> {
    BetHistory findByRoundIdAndVendorGameIdAndVendorPlayerId(String roundId, Integer vendorGameId, Long vendorPlayerId);

    BetHistory findByExternalTransactionIdAndVendorId(String txnId, Integer vendorId);

    BetHistory findByExternalTransactionIdAndVendorIdAndVendorPlayerId(String txnId, Integer vendorId, Long vendorPlayerId);

    BetHistory findByExternalTransactionIdAndRoundIdAndVendorLineId(String externalTransactionId, String roundId, Integer VendorLineId);


    @Query(value="SELECT " +
            "bh.id AS betId, " +
            "bh.round_id AS roundId, " +
            "bh.vendor_bet_id AS externalTransactionId, " +
            "ap.username AS username, " +
            "c.code AS currencyCode, " +
            "vg.code AS gameCode, " +
            "v.code AS vendorCode, " +
            "gc.code AS gameCategoryCode, " +
            "bh.bet_amount AS betAmount, " +
            "bh.win_amount AS winAmount, " +
            "bh.win_loss AS winLoss, " +
            "bh.effective_turnover AS effectiveTurnover, " +
            "bh.jackpot_amount AS jackpotAmount, " +
            "bh.status AS status, " +
            "bh.vendor_bet_time AS vendorBetTime, " +
            "bh.vendor_settle_time AS vendorSettleTime, " +
            "IF(bh.is_freespin =0 ,'FALSE','TRUE') AS isFreeSpin "+
            "FROM bet_history AS bh " +
            "INNER JOIN agent_players AS ap ON ap.id = bh.agent_player_id " +
            "INNER JOIN vendor_players AS vp ON vp.id = bh.vendor_player_id " +
            "INNER JOIN currencies AS c ON c.id = bh.currency_id " +
            "INNER JOIN vendor_games AS vg ON vg.id = bh.vendor_game_id " +
            "INNER JOIN vendors AS v ON v.id = bh.vendor_id " +
            "INNER JOIN game_categories AS gc ON gc.id = vg.game_category_id " +
            " WHERE bh.agent_id =:agentId AND bh.vendor_bet_time BETWEEN :fromTime AND :toTime ORDER BY bh.vendor_bet_time DESC ",
            countQuery =
                    "SELECT count(*) FROM bet_history WHERE agent_id =:agentId AND vendor_bet_time BETWEEN :fromTime AND :toTime",
            nativeQuery=true)
    Page<Object> findByAgentIdAndCreateTimeBetween(
            @Param("agentId") Integer agentId, @Param("fromTime") Long fromTime, @Param("toTime") Long toTime, Pageable pageable);

    @Query(value=" SELECT " +
            "bh.id AS betId, " +
            "bh.external_transaction_id AS transactionId, " +
            "bh.round_id AS externalRoundId, " +
            "bh.vendor_bet_id AS externalTransactionId, " +
            "ap.username AS username, " +
            "bh.currency_id AS currencyId, " +
            "vc.vendor_currency_code AS vendorCurrencyCode, " +
            "c.code AS currencyCode, " +
            "vg.code AS gameCode, " +
            "bh.vendor_id AS vendorId, " +
            "v.code AS vendorCode, " +
            "gc.code AS gameCategoryCode, " +
            "bh.bet_amount AS betAmount, " +
            "bh.win_amount AS winAmount, " +
            "bh.win_loss AS winLoss, " +
            "bh.effective_turnover AS effectiveTurnover, " +
            "bh.jackpot_amount AS jackpotAmount, " +
            "bh.status AS status, " +
            "bh.vendor_bet_time AS vendorBetTime, " +
            "bh.vendor_settle_time AS vendorSettleTime, " +
            "bh.vendor_line_id AS vendorLineId, " +
            "IF(bh.is_freespin =0 ,'TRUE','FALSE') AS isFreeSpin, "+
            "vp.username AS vendorUsername " +
            "FROM bet_history AS bh " +
            "INNER JOIN agent_players AS ap ON ap.id = bh.agent_player_id " +
            "INNER JOIN vendor_players AS vp ON vp.id = bh.vendor_player_id " +
            "INNER JOIN currencies AS c ON c.id = bh.currency_id " +
            "INNER JOIN vendor_games AS vg ON vg.id = bh.vendor_game_id " +
            "INNER JOIN vendors AS v ON v.id = bh.vendor_id " +
            "INNER JOIN game_categories AS gc ON gc.id = vg.game_category_id " +
            "INNER JOIN vendor_currencies AS vc ON vc.vendor_id = bh.vendor_id AND vc.currency_id = bh.currency_id " +
            "WHERE bh.id = :betId AND bh.agent_id = :agentId", nativeQuery=true)
    IBetDetailUrlInfo findByIdAndAgentId (
            @Param("agentId") int agentId,
            @Param("betId") String betId);
}
