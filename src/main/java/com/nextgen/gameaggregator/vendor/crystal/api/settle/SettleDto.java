package com.nextgen.gameaggregator.vendor.crystal.api.settle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.crystal.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SettleDto extends CommonDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactionId")
    private String transactionId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameCode")
    private String gameCode;

    @Override
    public String getExternalTransactionId() {
        return this.roundId;
    }

    @Override
    public String getVendorBetId() {
        return this.transactionId;
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
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
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
