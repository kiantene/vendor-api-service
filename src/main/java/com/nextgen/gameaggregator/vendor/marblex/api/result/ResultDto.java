package com.nextgen.gameaggregator.vendor.marblex.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultDto extends CommonDto implements SportBetResultData {
    @JsonProperty("RoundID")
    private String roundId;

    @JsonProperty("GameCode")
    private String gameCode;

    @JsonProperty("ReturnToWallet")
    private BigDecimal returnToWallet;

    @JsonProperty("Winloss")
    private BigDecimal winLoss;

    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;

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
        return null;
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.returnToWallet;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.winLoss;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
    }

    @Override
    public Long getVendorSettleTime() {
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }

    @Override
    public Integer getBetType() {
        return BetType.NORMAL_BET.code;
    }
}
