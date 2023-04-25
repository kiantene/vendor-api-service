package com.nextgen.gameaggregator.vendor.joker.api.jackpotwin;

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
public class JackpotWinDto implements BetResultData {

    @NotBlank(message = ResponseCodes.INVALID_APPID)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_APPID)
    private String appid;

    @NotBlank(message = ResponseCodes.INVALID_SIGNATURE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String hash;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String id;

    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Range(min = 0, message = ResponseCodes.INVALID_PARAMETERS)
    @Digits(integer = 12, fraction = 2, message = ResponseCodes.INVALID_PARAMETERS)
    private BigDecimal amount;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    @Size(min = 4, max = 32, message = ResponseCodes.INVALID_PARAMETERS)
    private String username;

    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.INVALID_PARAMETERS)
    private Long timestamp;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String gamecode;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String roundid;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String description;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
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
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
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
    public BigDecimal getJackpotAmount() { return this.amount;}

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
