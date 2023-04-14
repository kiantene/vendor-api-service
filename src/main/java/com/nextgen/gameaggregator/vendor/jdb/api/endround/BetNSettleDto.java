package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto implements UnsettledResultSettledData {

    private WinType resultType;

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
    private String gameSeqNo;

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

    @NotBlank(message = "WRONG_DATE_FORMAT")
    @Size(max = 10, message = "WRONG_DATE_FORMAT")
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4}$", message = "WRONG_DATE_FORMAT")
    private String reportDate;

    @NotBlank(message = "WRONG_DATE_FORMAT")
    @Size(max = 19, message = "WRONG_DATE_FORMAT")
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "WRONG_DATE_FORMAT")
    private String gameDate;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @Negative
    private BigDecimal bet;

    @NotNull
    @PositiveOrZero(message = "PARAMETER_CANNOT_BE_NEGATIVE")
    private BigDecimal win;

    @NotNull
    private BigDecimal netWin;

    @NotNull
    @PositiveOrZero(message = "PARAMETER_CANNOT_BE_NEGATIVE")
    private BigDecimal denom;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^(([01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d{1,2}|2[0-4]\\d|25[0-5])$|^(([a-fA-F\\d]{1,4}:){7}[a-fA-F\\d]{1,4}|([a-fA-F\\d]{1,4}:){1,7}:|([a-fA-F\\d]{1,4}:){6}:([01][a-fA-F\\d]{1,3}:){1,4}[a-fA-F\\d]{1,4}|([a-fA-F\\d]{1,4}:){5}:([01][a-fA-F\\d]{1,3}:){1,5}[a-fA-F\\d]{1,4}|([a-fA-F\\d]{1,4}:){4}:([01][a-fA-F\\d]{1,3}:){1,6}[a-fA-F\\d]{1,4}|([a-fA-F\\d]{1,4}:){3}:([01][a-fA-F\\d]{1,3}:){1,7}[a-fA-F\\d]{1,4}|([a-fA-F\\d]{1,4}:){2}:([01][a-fA-F\\d]{1,3}:){1,8}[a-fA-F\\d]{1,4}|[a-fA-F\\d]:([01][a-fA-F\\d]{1,3}:){1,8}:[a-fA-F\\d]{1,4}|:((:[a-fA-F\\d]{1,4}){1,7}|:)|fe80:(:[a-fA-F\\d]{0,4}){0,4}%[\\w\\d]+|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)|([a-fA-F\\d]{1,4}:){1,4}:((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?))$", message = "INVALID_IP_ADDRESS")
    private String ipAddress;

    @NotBlank
    @Size(max = 20)
    private String clientType;

    @NotNull
    @Min(value = 0)
    @Max(value = 1)
    private Integer systemTakeWin;

    @NotBlank(message = "WRONG_DATE_FORMAT")
    @Size(max = 19, message = "WRONG_DATE_FORMAT")
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "WRONG_DATE_FORMAT")
    private String lastModifyTime;

    @Size(max = 50)
    private String sessionNo;

    @NotNull
    private BigDecimal mb;


    // Slot Only, gType = 0
    @PositiveOrZero(message = "PARAMETER_CANNOT_BE_NEGATIVE")
    private BigDecimal jackpotWin;

    @Negative
    private BigDecimal jackpotContribute;

    @Min(value = 0)
    @Max(value = 1)
    private Integer hasFreeGame;


    // Fish Only, gType = 7
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String roomType;


    // Slot and Arcade, gType = 0 OR gType = 9
    @Min(value = 0)
    @Max(value = 1)
    private Integer hasGamble;


    // Arcade and Lottery, gType = 9 OR gType = 12
    @Min(value = 0)
    @Max(value = 1)
    private Integer hasBonusGame;

    @Override
    public String getExternalTransactionId() {
        return transferId;
    }

    public void setExternalTransactionId(String transferId) {
        this.transferId = transferId;
    }

    @Override
    public String getVendorBetId() {
        return transferId;
    }

    @Override
    public String getRoundId() {
        return gameSeqNo;
    }

    public void setRoundId(String gameSeqNo) {
        this.gameSeqNo = gameSeqNo;
    }

    @Override
    public String getGameId() {
        return mType;
    }

    public void setGameId(String mType) {
        this.mType = mType;
    }

    @Override
    public BigDecimal getBetAmount() {
        return bet;
    }

    @Override
    public BigDecimal getWinAmount() {
        return win;
    }

    public void setWinAmount(BigDecimal win) {
        this.win = win;
    }

    @Override
    public BigDecimal getWinLoss() {
        return netWin;
    }


    @Override
    public BigDecimal getEffectiveTurnover() {
        return bet;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public WinType getResultType() {
        return this.netWin.compareTo(BigDecimal.ZERO) > 0 ? WinType.WIN : WinType.LOSE;
    }

    @Override
    public Long getVendorBetTime() {
        return ts;
    }

    public void setVendorBetTime(Long ts) {
        this.ts = ts;
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
        return BigDecimal.ZERO;
    }

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
