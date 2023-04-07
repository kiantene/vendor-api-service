package com.nextgen.gameaggregator.vendor.jdb.api.result;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto implements UnsettledResultSettledData {
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
    @Size(min = 1, max = 30)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;
    
    @NotBlank
    @Size(max = 3)
    private String currency;

    @Positive
    private BigDecimal amount;

    private List<Long> refTransferIds;

    @Positive
    private Long gameRoundSeqNo;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String gameSeqNo;

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
    private String reportDate;

    @NotBlank
    @Size(max = 19)
    private String gameDate;

    @NotBlank
    @Size(max = 19)
    private String lastModifyTime;

    @NotNull
    @Negative
    private BigDecimal bet;

    @NotNull
    @Positive
    private BigDecimal validBet;

    @NotNull
    @PositiveOrZero
    private BigDecimal win;

    @NotNull
    private BigDecimal netWin;

    @Positive
    private BigDecimal tax;

    @NotBlank
    @Size(max = 50)
    private String sessionNo;

    @Override
    public String getExternalTransactionId() {
        return transferId;
    }

    public void setExternalTransactionId(String transferId) {
        this.transferId = transferId;
    }

    @Override
    public String getVendorBetId() {
        if (refTransferIds != null && !refTransferIds.isEmpty()) {
            return String.valueOf(refTransferIds.get(0));
        }
        return null;
    }

    @Override
    public String getRoundId() {
        return this.gameRoundSeqNo.toString();
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
