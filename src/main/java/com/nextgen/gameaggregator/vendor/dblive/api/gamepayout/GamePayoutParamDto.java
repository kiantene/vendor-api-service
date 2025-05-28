package com.nextgen.gameaggregator.vendor.dblive.api.gamepayout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GamePayoutParamDto implements BetResultData {
    @NotNull
    @Digits(integer = 15, fraction = 6)
    private BigDecimal payoutAmount;
    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long payoutTime;
    @NotBlank
    @Size(max = 19)
    private String gameTypeId;
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private BigDecimal transferNo;
    @NotBlank
    @Size(max = 50)
    private String loginName;
    @NotBlank
    @Size(max = 20)
    private String transferType;
    @NotBlank
    @Size(max = 3)
    private String currency;
    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String roundNo;

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.transferNo);
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.transferNo);
    }

    @Override
    public String getRoundId() {
        return this.roundNo;
    }

    @Override
    public String getGameId() {
        return this.gameTypeId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.payoutAmount;
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
        return this.payoutTime;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.payoutTime;
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
        return BetStatus.SETTLED;
    }
}
