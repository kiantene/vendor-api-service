package com.nextgen.gameaggregator.vendor.jdb.api.result;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto implements BetResultData {
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

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal amount;

    @Valid
    @NotNull
    @Size(min = 1, max = 30)
    private List<@NotNull Long> refTransferIds;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
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

    @NotBlank(message = ResponseCode.WRONG_DATE_FORMAT)
    @Size(max = 10)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4}$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String reportDate;

    @NotBlank(message = ResponseCode.WRONG_DATE_FORMAT)
    @Size(max = 19)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String gameDate;

    @NotBlank(message = ResponseCode.WRONG_DATE_FORMAT)
    @Size(max = 19)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String lastModifyTime;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal bet;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal validBet;

    @NotNull
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal win;

    @NotNull
    private BigDecimal netWin;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal tax;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String sessionNo;

    @Override
    public String getExternalTransactionId() {
        return refTransferIds.get(0).toString();
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
    public BigDecimal getEffectiveTurnover() {
        return bet;
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
