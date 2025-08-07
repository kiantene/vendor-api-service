package com.nextgen.gameaggregator.vendor.inout.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    private String amount;

    @NotBlank
    @Size(max = 255)
    private String result;

    @NotBlank
    @Size(max = 255)
    private String coefficient;

    @NotBlank
    @Size(max = 255)
    private String currency;

    @NotBlank
    @Size(max = 255)
    private String operator;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("user_id")
    private String userId;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    private String debitId;

    @NotBlank
    @Size(max = 255)
    private String gameId;


    @Override
    public String getExternalTransactionId() {
        return this.getDebitId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransactionId();
    }

    @Override
    public String getRoundId() {
        return this.getGameId();
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return new BigDecimal(getAmount());
    }

    @Override
    public BigDecimal getWinAmount() {
        return new BigDecimal(getResult());
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
        return System.currentTimeMillis();
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
        return BetStatus.SETTLED;
    }
}
