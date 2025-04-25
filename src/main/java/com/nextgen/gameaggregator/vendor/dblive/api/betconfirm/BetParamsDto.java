package com.nextgen.gameaggregator.vendor.dblive.api.betconfirm;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BetParamsDto implements BetResultData {

    @NotBlank
    @Size(max = 19)
    private String gameTypeId;
    @NotNull
    @Positive
    @Digits(integer = 15, fraction = 4)
    private BigDecimal betTotalAmount;
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private BigDecimal transferNo;
    @NotBlank
    @Size(max = 50)
    private String loginName;
    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long betTime;
    @NotBlank
    @Size(max = 3)
    private String currency;
    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String roundNo;

    private List<BetInfoDto> betInfo;

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
        return this.betTotalAmount;
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
        return this.betTotalAmount;
    }

    @Override
    public Long getVendorBetTime() {
        return this.betTime;
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
        return BigDecimal.ZERO;
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
