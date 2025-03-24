package com.nextgen.gameaggregator.vendor.marblex.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements SportBetResultData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("RoundID")
    private String roundId;

    @NotBlank
    @JsonProperty("GameCode")
    private String gameCode;

    @NotNull
    @JsonProperty("BetAmount")
    private BigDecimal betAmount;

    @NotBlank
    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;

    @NotBlank
    @JsonProperty("TransactionTime")
    private String transactionTime;

    @Override
    public String getExternalTransactionId() {
        return this.janusTransactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.janusTransactionId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.getPlayerId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getNewBetAmount() {
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
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
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
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }

    @Override
    public Integer getBetType() {
        return BetType.NORMAL_BET.code;
    }
}
