package com.nextgen.gameaggregator.vendor.queenmaker.api.reward;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RewardTransactionsDto implements BetResultData {
    @NotBlank(message = "userid cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid userid Format")
    @Size(min = 1, max = 50, message = "Invalid userid Size")
    private String userid;
    @NotNull(message = "amt cannot be empty")
    @Range(min = 0, message = "amt cannot less than 0")
    @Digits(integer = 12, fraction = 6, message = "Invalid amt Format")
    private BigDecimal amt;
    @NotBlank(message = "cur cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid cur Format")
    @Size(min = 3, max = 8, message = "Invalid cur Size")
    private String cur;
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid ptxid Format")
    @Size(min = 1, max = 36, message = "Invalid ptxid Size")
    private String ptxid;
    private String desc; // optional

    @Override
    public String getExternalTransactionId() {
        return this.ptxid;
    }

    @Override
    public String getVendorBetId() {
        return this.ptxid;
    }

    @Override
    public String getRoundId() {
        return this.ptxid;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amt;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.amt;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return VendorService.getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getTimestamp();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
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
