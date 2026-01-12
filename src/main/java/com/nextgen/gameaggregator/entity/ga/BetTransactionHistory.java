package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BetTransactionHistory {
//    @JsonProperty("id")
//    private String id;

    // Vendor Information
    @JsonProperty("external_transaction_id")
    private String externalTransactionId;

    @JsonProperty("vendor_bet_id")
    private String vendorBetId;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("vendor_game_id")
    private Integer vendorGameId;

    @JsonProperty("vendor_player_id")
    private Long vendorPlayerId;

    @JsonProperty("vendor_player_username")
    private String vendorPlayerUsername;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_code")
    private String vendorCode;

    @JsonProperty("vendor_line_id")
    private Integer vendorLineId;

    // Agent Information
    @JsonProperty("agent_player_id")
    private Long agentPlayerId;

    @JsonProperty("agent_player_username")
    private String agentPlayerUsername;

    @JsonProperty("house_id")
    private Integer houseId;

    @JsonProperty("master_agent_id")
    private Integer masterAgentId;

    @JsonProperty("agent_id")
    private Integer agentId;

    // GA Information
    @JsonProperty("gaBetId")
    private String gaBetId;

    @JsonProperty("game_category_id")
    private Integer gameCategoryId;

    @JsonProperty("game_category_code")
    private String gameCategoryCode;

    @JsonProperty("game_code")
    private String gameCode;

    @JsonProperty("currency_id")
    private Integer currencyId;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("transaction_type")
    private String transactionType;

    @JsonProperty("timestamp")
    private Long timestamp;

    //    @JsonProperty("is_freespin")
//    private Integer isFreespin;
//
//    @JsonProperty("resettle_num")
//    private Integer resettleNum;
//
//    @JsonProperty("status")
//    private Integer status;
//
//    @JsonProperty("game_session_token")
//    private String gameSessionToken;
}
