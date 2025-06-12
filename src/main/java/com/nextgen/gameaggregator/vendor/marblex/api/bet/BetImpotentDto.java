package com.nextgen.gameaggregator.vendor.marblex.api.bet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.constant.Formats;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class BetImpotentDto extends CommonDto implements BetResultData {
    @NotBlank
    @JsonProperty("Currency")
    @Size(max = 5)
    private String currency;

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
    @JsonProperty("BetAmount")
    private BigDecimal betAmount;

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
