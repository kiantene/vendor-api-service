package com.nextgen.gameaggregator.vendor.bgaming.api.endround;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EndRoundDto implements BetResultData {
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
    private String gameId;
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
        return this.betId;
    }

    @Override
    public String getVendorBetId() {
        return this.betId;
    }

    @Override
    public String getRoundId() {
        return this.gameId;
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
        return this.winAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return this.timestamp;
    }

    @Override
    public Long getResultTime() {
        return this.timestamp;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.timestamp;
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
        if (this.finished) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
