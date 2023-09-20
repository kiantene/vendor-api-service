package com.nextgen.gameaggregator.vendor.bgaming.api.bet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BetDto implements BetResultData {

    @NotBlank
    @JsonProperty("user_id")
    private String userId;
    @NotBlank
    @JsonProperty("currency")
    private String currency;
    @NotBlank
    @JsonProperty("game")
    private String game;
    @JsonProperty("game_id")
    private String vendorRoundId;
    @JsonProperty("finished")
    private Boolean finished;
    @JsonProperty("actions")
    private List<ActionDto> actions;
    private String betId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private Long timestamp;

    @Override
    public String getExternalTransactionId() {
        return this.vendorRoundId;
    }

    @Override
    public String getVendorBetId() {
        return this.betId;
    }

    @Override
    public String getRoundId() {
        return this.vendorRoundId;
    }

    @Override
    public String getGameId() {
        return this.game;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.betAmount;
    }

    @Override
    public Long getVendorBetTime() {
        return this.timestamp;
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
