package com.nextgen.gameaggregator.vendor.hacksaw.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.api.action.ActionDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditDto extends ActionDto implements BetResultData {

    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String externalPlayerId;

    @NotNull
    @PositiveOrZero
    private Long amount;

    @NotBlank
    @Size(min = 1, max = 4)
    private String currency;

    @NotNull
    private Long gameSessionId;

    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String externalSessionId;

    @NotNull
    private Long transactionId;

    @NotBlank
    @Size(min = 1, max = 4)
    @Pattern(regexp = "^(real|free)$")
    private String type;

    // variable to check it is free spin or not (ONLY if set thru BO)
    private FreeRoundDto freeRoundData;

    private Long jackpotAmount;

    private Boolean ended;

    private Long betTransactionId;

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.getBetTransactionId());
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.getBetTransactionId());
    }

    @Override
    public String getRoundId() {
        return super.getRoundId(); // prevent crash name
    }

    @Override
    public String getGameId() {
        return super.getGameId(); // prevent crash name
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return new BigDecimal(this.getAmount());
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
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {

        Integer status = 0;

        if (this.getFreeRoundData() != null && this.getType().equals("free")) {
            status = 1;
        }

        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
