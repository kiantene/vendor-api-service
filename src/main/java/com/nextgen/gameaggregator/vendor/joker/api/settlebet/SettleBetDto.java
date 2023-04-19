package com.nextgen.gameaggregator.vendor.joker.api.settlebet;

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
public class SettleBetDto implements UnsettledResultSettledData {

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String appid;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String hash;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String id;

    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Range(min = 0, message = ResponseCodes.INVALID_PARAMETERS)
    @Digits(integer = 12, fraction = 2, message = ResponseCodes.INVALID_PARAMETERS)
    private BigDecimal amount;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    @Size(min = 4, max = 32, message = ResponseCodes.INVALID_PARAMETERS)
    private String username;

    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.INVALID_SIGNATURE)
    private Long timestamp;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String gamecode;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String roundid;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String description;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String type;

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
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return (this.amount.subtract(this.getBetAmount()));
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public WinType getResultType() {
        return (this.getWinAmount().compareTo(BigDecimal.ZERO) > 0)?WinType.WIN:WinType.LOSE;
    }

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
