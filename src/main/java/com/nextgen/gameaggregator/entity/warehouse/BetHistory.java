package com.nextgen.gameaggregator.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.math.BigDecimal;

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

    @JsonProperty("game_code")
    private String gameCode;

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

    @JsonProperty("agent_player_id")
    private Long agentPlayerId;

    @JsonProperty("agent_player_username")
    private String agentPlayerUsername;

    @JsonProperty("agent_id")
    private Integer agentId;

    @JsonProperty("operator_status")
    private Integer operatorStatus;

    @JsonProperty("game_category_id")
    private Integer gameCategoryId;

    @JsonProperty("game_category_code")
    private String gameCategoryCode;

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

    public BetHistory
            (com.nextgen.gameaggregator.entity.ga.BetHistory originBetHistory,
             String agentPlayerUsername, String vendorPlayerUsername,
             WarehouseFutureEntity warehouseFutureEntity) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(originBetHistory, this);

        this.agentPlayerUsername = agentPlayerUsername;
        this.vendorPlayerUsername = vendorPlayerUsername;
        this.currencyCode = warehouseFutureEntity.getCurrency().getCode();
        this.gameCode = warehouseFutureEntity.getVendorGame().getCode();
        this.gameCategoryCode = warehouseFutureEntity.getGameCategory().getCode();
        this.vendorCode = warehouseFutureEntity.getVendor().getCode();
    }
}
