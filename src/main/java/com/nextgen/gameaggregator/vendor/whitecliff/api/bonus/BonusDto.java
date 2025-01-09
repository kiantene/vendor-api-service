package com.nextgen.gameaggregator.vendor.whitecliff.api.bonus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusDto implements BetResultData {


    @NotNull
    @JsonProperty("user_id")
    @Digits(integer = 50, fraction = 0)
    private BigInteger userId;

    @NotNull
    @JsonProperty("type")
    @Digits(integer = 50, fraction = 0)
    private BigInteger type;

    @NotNull
    @JsonProperty("amount")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal amount;

    @NotNull
    @JsonProperty("prd_id")
    @Digits(integer = 20, fraction = 0)
    private BigInteger prdId;

    @NotBlank
    @JsonProperty("txn_id")
    @Size(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_.-]*$")
    private String txnId;

    @NotNull
    @JsonProperty("game_id")
    private BigInteger gameId;

    @JsonIgnore
    private Integer gameCategory;

    @NotBlank
    @Size(max = 255)
    private String sid;

    @Override
    public String getExternalTransactionId() {
        return this.txnId;
    }

    @Override
    public String getVendorBetId() {
        return this.txnId;
    }

    @Override
    public String getRoundId() {
        return this.txnId;
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.gameId);
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
    public Long getVendorBetTime() { return System.currentTimeMillis(); }

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
        return BetStatus.SETTLED;
    }
}
