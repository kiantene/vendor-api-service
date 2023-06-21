package com.nextgen.gameaggregator.vendor.joker.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetDto implements BetResultData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String appid;

    @NotBlank(message = ResponseCodes.INVALID_SIGNATURE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String hash;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String id;

    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 4, max = 32)
    private String username;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long timestamp;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gamecode;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String roundid;

    @Override
    public String getExternalTransactionId() {
        return this.username + "_" + this.id;
    }

    @Override
    public String getVendorBetId() {
        return this.username + "_" + this.id;
    }

    @Override
    public String getRoundId() {
        return this.username + "_" + this.roundid;
    }

    @Override
    public String getGameId() {
        return this.gamecode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.amount;
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
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() { return null;}

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
