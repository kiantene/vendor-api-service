package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BetResultDlq {
    @JsonProperty("vendor_id")
    private Integer vendorId;
    @JsonProperty("vendor_player_id")
    private Long vendorPlayerId;
    @JsonProperty("agent_id")
    private Integer agentId;
    @JsonProperty("agent_player_id")
    private Long agentPlayerId;
    @JsonProperty("vendor_game_id")
    private Integer vendorGameId;
    @JsonProperty("game_category_id")
    private Integer gameCategoryId;
    @JsonProperty("currency_id")
    private Integer currencyId;
    @JsonProperty("game_session_token")
    private String gameSessionToken;

    // Fields from BetResultData
    @JsonProperty("external_transaction_id")
    private String externalTransactionId;
    @JsonProperty("vendor_bet_id")
    private String vendorBetId;
    @JsonProperty("round_id")
    private String roundId;
    @JsonProperty("game_id")
    private String gameId;
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
    @JsonProperty("vendor_bet_time")
    private Long vendorBetTime;
    @JsonProperty("result_time")
    private Long resultTime;
    @JsonProperty("vendor_settle_time")
    private Long vendorSettleTime;
    @JsonProperty("is_free_spin")
    private Integer isFreespin;
    @JsonProperty("bet_status")
    private BetStatus betStatus;
    @JsonProperty("request_time")
    private Long requestTime;

    public BetResultDlq(BetResultData betResultData) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(betResultData, this);
    }
}
