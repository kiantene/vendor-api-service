package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bet_refund_log")
@Data
@NoArgsConstructor
public class BetRefundLog {
    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("bet_history_id")
    private String betHistoryId;

    @JsonProperty("external_transaction_id")
    private String externalTransactionId;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("vendor_line_id")
    private Integer vendorLineId;

    @JsonProperty("vendor_game_id")
    private Integer vendorGameId;

    @JsonProperty("vendor_player_id")
    private Long vendorPlayerId;

    @JsonProperty("agent_player_id")
    private Long agentPlayerId;

    @JsonProperty("agent_id")
    private Integer agentId;

    @JsonProperty("operator_status")
    private Integer operatorStatus;

    @JsonProperty("currency_id")
    private Integer currencyId;

    @JsonProperty("create_time")
    private Long createTime;

    public BetRefundLog(RawBetRefundLog rawBetRefundLog) {
        this.id = rawBetRefundLog.getBetRefundLogId();
        this.betHistoryId = rawBetRefundLog.getBetHistoryId();
        this.agentId = rawBetRefundLog.getAgentId();
        this.agentPlayerId = rawBetRefundLog.getAgentPlayerId();
        this.createTime = rawBetRefundLog.getCreateTime();
        this.currencyId = rawBetRefundLog.getCurrencyId();
        this.externalTransactionId = rawBetRefundLog.getExternalTransactionId();
        this.operatorStatus = rawBetRefundLog.getOperatorStatus();
        this.roundId = rawBetRefundLog.getRoundId();
        this.vendorGameId = rawBetRefundLog.getVendorGameId();
        this.vendorLineId = rawBetRefundLog.getVendorLineId();
        this.vendorPlayerId = rawBetRefundLog.getVendorPlayerId();
    }
}
