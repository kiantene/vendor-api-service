package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import java.math.BigDecimal;

import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^[0-9-]+$") // only accept numbers and dashes
    private String reportDate;

    @NotBlank
    @Size(max = 19)
    @Pattern(regexp = "^[0-9:-\\s]+$")
    private String gameDate;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @Negative
    private BigDecimal bet;

    @NotNull
    @PositiveOrZero
    private BigDecimal win;

    @NotNull
    private BigDecimal netWin;

    @NotNull
    @PositiveOrZero
    private BigDecimal denom;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
    private String ipAddress;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String clientType;

    @NotNull
    @Size(min = 0, max = 1)
    private Integer systemTakeWin;

    @NotBlank
    @Size(max = 19)
    @Pattern(regexp = "^[0-9:-\\s]+$")
    private String lastModifyTime;

    @Size(max = 50)
    private String sessionNo;

    @NotNull
    private BigDecimal mb;


    // Slot Only, gType = 0
    private BigDecimal jackpotWin;

    private BigDecimal jackpotContribute;

    @Size(min = 0, max = 1)
    @JsonProperty("hasFreegame")
    private Integer hasFreeGame;


    // Fish Only, gType = 7
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String roomType;


    // Slot and Arcade, gType = 0 OR gType = 9
    @Size(min = 0, max = 1)
    private Integer hasGamble;


    // Arcade and Lottery, gType = 9 OR gType = 12
    @Size(min = 0, max = 1)
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
    public BigDecimal getVendorWinLoss() {
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
    public Integer getIsCancelled() {
        return 0;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }
}
