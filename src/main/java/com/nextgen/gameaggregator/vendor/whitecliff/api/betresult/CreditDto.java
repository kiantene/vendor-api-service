package com.nextgen.gameaggregator.vendor.whitecliff.api.betresult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditDto implements BetResultData, RollbackData {

    @NotNull
    @JsonProperty("user_id")
    @Digits(integer = 50, fraction = 0)
    private BigInteger userId;

    @NotNull
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

    @NotBlank
    @JsonProperty("round_id")
    @Size(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_.-]*$")
    private String roundId;

    @NotNull
    @Digits(integer = 50, fraction = 0)
    @JsonProperty("game_id")
    private BigInteger gameId;

    @NotNull
    @JsonProperty("is_cancel")
    @Min(value = 0)
    @Max(value = 1)
    private Integer isCancel;

    @NotBlank
    @JsonProperty("table_id")
    @Size(max = 255)
    private String tableId;

    @NotBlank
    @JsonProperty("credit_time")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")
    private String creditTime;

    @NotBlank
    @JsonProperty("sid")
    @Size(max = 255)
    private String sid;

    @JsonIgnore
    public Integer gameCategory = 0;

    @Override
    public String getExternalTransactionId() {
        return this.txnId;
    }

    @Override
    public String getVendorBetId() {
        return this.txnId;
    }

    @Override
    public String getRollbackId() {
        return this.txnId;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        if (gameCategory == 5){
            return this.tableId;
        }
        return String.valueOf(this.gameId);
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getAmount();
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
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() { return null; }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
