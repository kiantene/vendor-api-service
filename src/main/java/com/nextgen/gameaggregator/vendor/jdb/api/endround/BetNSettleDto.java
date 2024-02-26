package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto implements BetResultData {

    private ResultType resultType;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String action;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long ts;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String transferId;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String historyId;

    @NotBlank
    @Size(min = 1, max = 30)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;

    @NotBlank
    @JsonProperty("gType")
    @Pattern(regexp = "^[0-9]+$")
    private String gType;

    @NotBlank
    @JsonProperty("mType")
    @Pattern(regexp = "^[0-9]+$")
    private String mType;

    @Size(max = 10, message = ResponseCode.WRONG_DATE_FORMAT)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4}$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String reportDate;

    @Size(max = 19, message = ResponseCode.WRONG_DATE_FORMAT)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String gameDate;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @NegativeOrZero
    private BigDecimal bet;

    @NotNull
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal win;

    @NotNull
    private BigDecimal netWin;

    private BigDecimal denom;

    @Size(max = 50)
    private String ipAddress;

    @Size(max = 20)
    private String clientType;

    @Min(value = 0)
    @Max(value = 1)
    private Integer systemTakeWin;

    @Size(max = 19, message = ResponseCode.WRONG_DATE_FORMAT)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String lastModifyTime;

    @Size(max = 50)
    private String sessionNo;

    private BigDecimal mb;

    // Slot Only, gType = 0
    private BigDecimal jackpotWin;

    private BigDecimal jackpotContribute;

    private Integer hasFreeGame;

    // Fish Only, gType = 7
    private String roomType;

    // Slot and Arcade, gType = 0 OR gType = 9
    private Integer hasGamble;

    // Arcade and Lottery, gType = 9 OR gType = 12
    private Integer hasBonusGame;

    @Override
    public String getExternalTransactionId() {
        return transferId;
    }

    @Override
    public String getVendorBetId() {
        return transferId;
    }

    @Override
    public String getRoundId() {
        return historyId;
    }

    @Override
    public String getGameId() {
        return mType;
    }

    @Override
    public BigDecimal getBetAmount() {
        return bet.negate();
    }

    @Override
    public BigDecimal getWinAmount() {
        return win;
    }

    @Override
    public BigDecimal getWinLoss() {
        return netWin.negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return bet.negate();
    }

    @Override
    public Long getVendorBetTime() {
        return ts;
    }

    @Override
    public Long getResultTime() {
        return ts;
    }

    @Override
    public Long getVendorSettleTime() {
        return ts;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return (getJackpotWin() != null) ? getJackpotWin() : BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return (getHasFreeGame() != null) ? getHasFreeGame() : 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
