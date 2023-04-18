package com.nextgen.gameaggregator.vendor.joker.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetDto implements UnsettledResultSettledData {

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String appid;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String hash;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String id;

    @NotNull(message = ResponseCodes.OTHER_MESSAGE)
    @Range(min = 0, message = ResponseCodes.OTHER_MESSAGE)
    @Digits(integer = 12, fraction = 2, message = ResponseCodes.OTHER_MESSAGE)
    private BigDecimal amount;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    @Size(min = 4, max = 32, message = ResponseCodes.OTHER_MESSAGE)
    private String username;

    @NotNull(message = ResponseCodes.OTHER_MESSAGE)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.OTHER_MESSAGE)
    private Long timestamp;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String gamecode;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String roundid;

    @Override
    public String getExternalTransactionId() {
        return this.id;
    }

    @Override
    public String getVendorBetId() {
        return this.id;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
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
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return getBetAmount();
    }

    @Override
    public WinType getResultType() { return WinType.LOSE; }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.valueOf(0);
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
        return getTimestamp();
    }

    @Override
    public BigDecimal getJackpotAmount() { return BigDecimal.ZERO;}

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
