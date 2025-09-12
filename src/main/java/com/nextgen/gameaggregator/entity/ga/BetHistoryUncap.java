package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class BetHistoryUncap extends BetHistoryV3 {

    @JsonProperty("uncap_win_amount")
    private BigDecimal uncapWinAmount;

    @JsonProperty("uncap_win_loss")
    private BigDecimal uncapWinLoss;

    @JsonProperty("uncap_jackpot_amount")
    private BigDecimal uncapJackpotAmount;

    @JsonProperty("uncap_effective_turnover")
    private BigDecimal uncapEffectiveTurnover;

    public static BetHistoryUncap copyOf(BetHistoryV3 source) {
        if (source == null) {
            return null;
        }

        BetHistoryUncap copy = new BetHistoryUncap();

        copy.setId(source.getId());
        copy.setExternalTransactionId(source.getExternalTransactionId());
        copy.setVendorBetId(source.getVendorBetId());
        copy.setRoundId(source.getRoundId());
        copy.setProductId(source.getProductId());
        copy.setProductCode(source.getProductCode());
        copy.setProductGameId(source.getProductGameId());
        copy.setVendorGameId(source.getVendorGameId());
        copy.setVendorPlayerId(source.getVendorPlayerId());
        copy.setVendorId(source.getVendorId());
        copy.setVendorCode(source.getVendorCode());
        copy.setVendorLineId(source.getVendorLineId());
        copy.setAgentPlayerId(source.getAgentPlayerId());
        copy.setHouseId(source.getHouseId());
        copy.setMasterAgentId(source.getMasterAgentId());
        copy.setAgentId(source.getAgentId());
        copy.setOperatorStatus(source.getOperatorStatus());
        copy.setGameCategoryId(source.getGameCategoryId());
        copy.setCurrencyId(source.getCurrencyId());
        copy.setCurrencyCode(source.getCurrencyCode());
        copy.setBetAmount(source.getBetAmount());
        copy.setWinAmount(source.getWinAmount());
        copy.setWinLoss(source.getWinLoss());
        copy.setEffectiveTurnover(source.getEffectiveTurnover());
        copy.setJackpotAmount(source.getJackpotAmount());
        copy.setResultType(source.getResultType());
        copy.setBetType(source.getBetType());
        copy.setIsFreespin(source.getIsFreespin());
        copy.setResettleNum(source.getResettleNum());
        copy.setStatus(source.getStatus());
        copy.setGameSessionToken(source.getGameSessionToken());
        copy.setVendorBetTime(source.getVendorBetTime());
        copy.setVendorSettleTime(source.getVendorSettleTime());
        copy.setResultTime(source.getResultTime());
        copy.setGameCode(source.getGameCode());
        copy.setVendorPlayerUsername(source.getVendorPlayerUsername());
        copy.setAgentPlayerUsername(source.getAgentPlayerUsername());
        copy.setGameCategoryCode(source.getGameCategoryCode());

        return copy;
    }
}
