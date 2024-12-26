package com.nextgen.gameaggregator.vendor.poker365.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("userId")
    private String userId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("currency")
    private String currency;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameNumber")
    private String gameNumber;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("txId")
    private String txId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameId")
    private String gameId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JsonProperty("profit")
    private BigDecimal profit;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("bonus")
    private BigDecimal bonus;

    @Override
    public String getExternalTransactionId() {
        return this.txId;
    }

    @Override
    public String getVendorBetId() {
        return this.txId;
    }

    @Override
    public String getRoundId() {
        return this.gameNumber;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.profit;
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
        return null;
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return this.bonus;
    }

    @Override
    public Integer getIsFreespin() {
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
