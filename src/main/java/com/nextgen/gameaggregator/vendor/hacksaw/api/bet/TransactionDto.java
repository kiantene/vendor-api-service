package com.nextgen.gameaggregator.vendor.hacksaw.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.api.action.ActionDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDto extends ActionDto implements BetResultData {

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
//    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private Long transactionId;

    // variable to check it is free spin or not
    private FreeRoundDto freeRoundData;

    @Size(min = 1, max = 64)
    private String boughtBonus;

    private Long baseBetLevel;

    public void checkRoundId() throws InvalidRequestException {
        if (super.getRoundId() == null) {
            throw new InvalidRequestException();
        }
    }

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.getTransactionId());
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.getTransactionId());
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
        return new BigDecimal(this.getAmount());
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return new BigDecimal(this.getAmount());
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
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
        return null;
    }

    @Override
    public Integer getIsFreespin() {

        Integer status = 0;

        if ((this.getFreeRoundData() != null && this.getAmount().equals(0)) || this.getBoughtBonus() != null) {
            status = 1;
        }

        return status;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
