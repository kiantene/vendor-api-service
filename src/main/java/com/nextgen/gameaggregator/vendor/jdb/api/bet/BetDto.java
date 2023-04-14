package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import lombok.Data;

@Data
public class BetDto implements UnsettledResultSettledData {
    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String action;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long ts;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String transferId;

    @NotBlank
    @Size(min = 1, max = 30)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;
    
    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @Positive(message = "PARAMETER_CANNOT_BE_NEGATIVE")
    private BigDecimal amount;

    @NotNull
    @Positive(message = "PARAMETER_CANNOT_BE_NEGATIVE")
    private Long gameRoundSeqNo;

    @NotBlank
    @JsonProperty("mType")
    @Pattern(regexp = "^[0-9]+$")
    private String mType;

    private BigDecimal effectiveTurnover;

    @Override
    public String getExternalTransactionId() {
        return this.transferId.toString();
    }

    @Override
    public String getRoundId() {
        return this.gameRoundSeqNo.toString();
    }

    @Override
    public String getGameId() {
        return this.mType.toString();
    }

    public Long getTimestamp() {
        return this.ts;
    }

    @Override
    public String getVendorBetId() {
        return this.transferId.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return effectiveTurnover;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public WinType getResultType() {
        return WinType.LOSE;
    }

    @Override
    public Long getVendorBetTime() {
        return ts;
    }

    public void setVendorBetTime(Long ts) {
        this.ts = ts;
    }

    @Override
    public Long getResultTime() {
        return ts;
    }

    @Override
    public Long getVendorSettleTime() {
        return ts;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
