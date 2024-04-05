package com.nextgen.gameaggregator.vendor.winfinity.api.result;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayoutDto implements BetResultData {
    @NotBlank
    @Size(max = 32)
    private String tid;

    @NotBlank
    @Size(max = 24)
    private String tbid;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;

    @NotBlank
    @Size(max = 4)
    private String cur;

    @Size(max = 10)
    private String gtp;

    @NotBlank
    @Size(max = 32)
    private String sid;

    @Size(max = 32)
    private String msid;

    @NotBlank
    @Size(max = 32)
    private String gid;

    @NotNull
    @PositiveOrZero
    private BigDecimal sum;

    private Long timestamp;

    @Size(max = 32)
    private String refid;

    @Override
    public String getExternalTransactionId() {
        return tid;
    }

    @Override
    public String getVendorBetId() {
        return tid;
    }

    @Override
    public String getRoundId() {
        return gid;
    }

    @Override
    public String getGameId() {
        return tbid;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return sum;
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
        return (timestamp != null) ? timestamp / 1000L : System.currentTimeMillis();
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
