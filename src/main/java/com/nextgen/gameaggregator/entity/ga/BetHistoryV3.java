package com.nextgen.gameaggregator.entity.ga;

import java.math.BigDecimal;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;

import lombok.Data;

@Data
public class BetHistoryV3 {
    @JsonProperty("id")
    private String id;

    @JsonProperty("external_transaction_id")
    private String externalTransactionId;

    @JsonProperty("vendor_bet_id")
    private String vendorBetId;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("product_game_id")
    private Integer productGameId;

    @JsonProperty("vendor_game_id")
    private Integer vendorGameId;

    @JsonProperty("vendor_player_id")
    private Long vendorPlayerId;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_code")
    private String vendorCode;

    @JsonProperty("vendor_line_id")
    private Integer vendorLineId;

    @JsonProperty("agent_player_id")
    private Long agentPlayerId;
    
    @JsonProperty("house_id")
    private Integer houseId;

    @JsonProperty("master_agent_id")
    private Integer masterAgentId;

    @JsonProperty("agent_id")
    private Integer agentId;

    @JsonProperty("operator_status")
    private Integer operatorStatus;

    @JsonProperty("game_category_id")
    private Integer gameCategoryId;

    @JsonProperty("currency_id")
    private Integer currencyId;

    @JsonProperty("currency_code")
    private String currencyCode;

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

    @JsonProperty("game_code")
    private String gameCode;

    @JsonProperty("vendor_player_username")
    private String vendorPlayerUsername;

    @JsonProperty("agent_player_username")
    private String agentPlayerUsername;

    @JsonProperty("game_category_code")
    private String gameCategoryCode;

    public BetHistoryV3 (BetHistory betHistory, String productCode, Integer productId, Integer productGameId, String agentPlayerUsername, 
        String vendorPlayerUsername, WarehouseFutureEntity warehouseFutureEntity) {

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(betHistory, this);

        this.productCode = productCode;
        this.productId = productId;
        this.productGameId = productGameId;
        this.agentPlayerUsername = agentPlayerUsername;
        this.vendorPlayerUsername = vendorPlayerUsername;
        this.currencyCode = warehouseFutureEntity.getCurrency().getCode();
        // TODO : Need to update logic to set product game code
        this.gameCode = warehouseFutureEntity.getVendorGame().getCode();
        this.gameCategoryCode = warehouseFutureEntity.getGameCategory().getCode();
        this.vendorCode = warehouseFutureEntity.getVendor().getCode();
        this.houseId = warehouseFutureEntity.getAgent().getHouseId();
        this.masterAgentId = warehouseFutureEntity.getAgent().getMasterAgentId();
    }
}
