package com.nextgen.gameaggregator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "bet_history")
@Data
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

    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;

    @JsonProperty("jackpot_amount")
    private BigDecimal jackpotAmount;

    @JsonProperty("result_type")
    private Integer resultType;

    @JsonProperty("is_freespin")
    private Integer isFreespin;

    @JsonProperty("raw_data")
    private String rawData;

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

    @JsonProperty("create_time")
    private Long createTime;

    @JsonProperty("result_time")
    private Long resultTime;
}
