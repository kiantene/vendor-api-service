package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "bet_history")
@Data
@NoArgsConstructor
public class BetHistory {
    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("external_transaction_id")
    private String externalTransactionId;

    @JsonProperty("vendor_bet_id")
    private String vendorBetId;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("vendor_game_id")
    private Integer vendorGameId;

    @JsonProperty("vendor_player_id")
    private Long vendorPlayerId;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_line_id")
    private Integer vendorLineId;

    @JsonProperty("agent_player_id")
    private Long agentPlayerId;

    @JsonProperty("agent_id")
    private Integer agentId;

    @JsonProperty("operator_status")
    private Integer operatorStatus;

    @JsonProperty("game_category_id")
    private Integer gameCategoryId;

    @JsonProperty("currency_id")
    private Integer currencyId;

    @JsonProperty("bet_amount")
    private BigDecimal betAmount;

    @JsonProperty("win_amount")
    private BigDecimal winAmount;

    @JsonProperty("win_loss")
    private BigDecimal winLoss;

    @JsonProperty("effective_turnover")
    private BigDecimal effectiveTurnover;

    @JsonProperty("jackpot_amount")
    private BigDecimal jackpotAmount;

    @JsonProperty("result_type")
    private Integer resultType;

    //temporary hardcode for all type of game category.
    @JsonProperty("bet_type")
    private Integer betType = 1;

    @JsonProperty("is_freespin")
    private Integer isFreespin;

    @JsonProperty("resettle_num")
    private Integer resettleNum;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("game_session_token")
    private String gameSessionToken;

    @JsonProperty("vendor_bet_time")
    private Long vendorBetTime;

    @JsonProperty("vendor_settle_time")
    private Long vendorSettleTime;

    @JsonProperty("result_time")
    private Long resultTime;

    public BetHistory(SettledBet settledBet) {
        this.id = settledBet.getBetId();
        this.externalTransactionId = settledBet.getExternalTransactionId();
        this.vendorBetId = settledBet.getVendorBetId();
        this.roundId = settledBet.getRoundId();
        this.vendorGameId = settledBet.getVendorGameId();
        this.vendorPlayerId = settledBet.getVendorPlayerId();
        this.vendorId = settledBet.getVendorId();
        this.vendorLineId = settledBet.getVendorLineId();
        this.agentPlayerId = settledBet.getAgentPlayerId();
        this.agentId = settledBet.getAgentId();
        this.operatorStatus = settledBet.getOperatorStatus();
        this.gameCategoryId = settledBet.getGameCategoryId();
        this.currencyId = settledBet.getCurrencyId();
        this.betAmount = settledBet.getBetAmount();
        this.winAmount = settledBet.getWinAmount();
        this.winLoss = settledBet.getWinLoss();
        this.effectiveTurnover = settledBet.getEffectiveTurnover();
        this.jackpotAmount = settledBet.getJackpotAmount();
        this.resultType = settledBet.getResultType();
        this.isFreespin = settledBet.getIsFreespin();
        this.status = settledBet.getStatus();
        this.gameSessionToken = settledBet.getGameSessionToken();
        this.vendorBetTime = settledBet.getVendorBetTime();
        this.vendorSettleTime = settledBet.getVendorSettleTime();
        this.resultTime = settledBet.getResultTime();

        if (settledBet.getResettleNum() == null) {
            settledBet.setResettleNum(0);
        }

        this.resettleNum = settledBet.getResettleNum();

        //hotfix v1.1.77, only for PlayNGo missing vendorBetTime.
        if (this.getVendorId().equals(29)) {
            this.vendorBetTime = (this.vendorBetTime == null) ? System.currentTimeMillis() : this.vendorBetTime;
            this.vendorSettleTime = (this.vendorSettleTime == null) ? System.currentTimeMillis() : this.vendorSettleTime;
            this.resultTime = (this.resultTime == null) ? System.currentTimeMillis() : this.resultTime;
        }
    }
}
