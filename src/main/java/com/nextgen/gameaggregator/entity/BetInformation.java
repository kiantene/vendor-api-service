package com.nextgen.gameaggregator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public abstract class BetInformation {
    @JsonProperty("id")
    private String id;

    @JsonIgnore
    private String internalTransactionId;

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

    @JsonProperty("currency_id")
    private Integer currencyId;

    @JsonProperty("bet_amount")
    private BigDecimal betAmount;

    @JsonProperty("win_amount")
    private BigDecimal winAmount;

    @JsonProperty("jackpot_amount")
    private BigDecimal jackpotAmount;

    @JsonProperty("win_loss")
    private BigDecimal winLoss;

    @JsonProperty("effective_turnover")
    private BigDecimal effectiveTurnover;

    @JsonProperty("result_type")
    private Integer resultType;

    @JsonProperty("is_freespin")
    private Integer isFreespin;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("game_session_token")
    private String gameSessionToken;

    @JsonProperty("game_category_id")
    private Integer gameCategoryId;

    @JsonProperty("vendor_bet_time")
    private Long vendorBetTime;

    @JsonProperty("vendor_settle_time")
    private Long vendorSettleTime;

    @JsonProperty("create_time")
    private Long createTime;

    @JsonProperty("result_time")
    private Long resultTime;
}
