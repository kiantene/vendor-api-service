package com.nextgen.gameaggregator.vendor.marblex.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.constant.Formats;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultDto extends CommonDto implements SportBetResultData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("RoundID")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("GameCode")
    private String gameCode;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("ReturnToWallet")
    private BigDecimal returnToWallet;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Winloss")
    private BigDecimal winLoss;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;
    
    @NotBlank
    @Pattern(regexp = Formats.TIME_REGEX)
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
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
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
