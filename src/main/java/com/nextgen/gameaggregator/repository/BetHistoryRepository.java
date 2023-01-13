package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.BetHistory;
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

    BetHistory findByExternalTransactionIdAndRoundIdAndVendorLineId(String externalTransactionId, String roundId, Integer VendorLineId);


    @Query(value=" SELECT "+
            "bh.id AS transaction_id, bh.round_id AS external_round_id, bh.external_transaction_id, "+
            "bh.vendor_game_id AS game_id, bh.bet_amount, bh.win_amount, bh.win_loss, bh.effective_turnover, "+
            "bh.result_type, bh.status, bh.vendor_bet_time, bh.vendor_settle_time, bh.create_time, "+
            "ap.username, gc.code AS category_code, v.code AS vendor_code, c.code AS currency, vg.code AS game_code "+
            "FROM bet_history AS bh "+
            "INNER JOIN agent_players AS ap ON bh.agent_player_id = ap.id "+
            "INNER JOIN game_categories AS gc ON bh.game_category_id = gc.id "+
            "INNER JOIN vendors AS v ON bh.vendor_id = v.id "+
            "INNER JOIN currencies AS c ON bh.currency_id = c.id "+
            "INNER JOIN vendor_games AS vg ON bh.vendor_game_id = vg.id "+
            " WHERE bh.agent_id =:agentId AND bh.create_time BETWEEN :fromTime AND :toTime ORDER BY bh.create_time DESC ",
            countQuery =
                    "SELECT count(*) FROM bet_history WHERE agent_id =:agentId AND create_time BETWEEN :fromTime AND :toTime",
            nativeQuery=true)
    Page<Object> findByAgentIdAnAndCreateTimeBetween(
            @Param("agentId") Integer agentId, @Param("fromTime") Long fromTime, @Param("toTime") Long toTime, Pageable pageable);

}
