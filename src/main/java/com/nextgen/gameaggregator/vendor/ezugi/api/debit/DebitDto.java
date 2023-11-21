package com.nextgen.gameaggregator.vendor.ezugi.api.debit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto extends CommonDto implements BetResultData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String uid;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String transactionId;
    @NotNull
    @Digits(integer = 18, fraction = 0)
    @JsonProperty("roundId")
    private BigInteger vendorRoundId;
    @NotNull
    @Digits(integer = 4, fraction = 0)
    @JsonProperty("gameId")
    private Integer vendorGameId;
    @NotNull
    private Integer tableId;
    @NotNull
    @PositiveOrZero(message = "Negative amount")
    @Digits(integer = 25, fraction = 2, message = "Invalid amount")
    private Double debitAmount;
    @NotNull
    @Digits(integer = 4, fraction = 0)
    private Integer betTypeID;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 4)
    private String currency;

    @Override
    public String getExternalTransactionId() {
        return this.transactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.transactionId;
    }

    @Override
    @JsonIgnore
    public String getRoundId() {
        return this.transactionId;
    }

    @Override
    @JsonIgnore
    public String getGameId() {
        return this.vendorGameId.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.valueOf(this.debitAmount);
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
        return getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        if (betTypeID.equals(BetTypeID.DEBIT_TIP)) {
            return getTimestamp();
        }
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        if (betTypeID.equals(BetTypeID.DEBIT_TIP)) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
