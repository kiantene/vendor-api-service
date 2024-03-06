package com.nextgen.gameaggregator.vendor.jdb.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.Formats;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal amount;

    @Valid
    @NotNull
    @Size(min = 1, max = 30)
    private List<@NotNull Long> refTransferIds;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private Long gameRoundSeqNo;

    @Pattern(regexp = "^[0-9]+$")
    private String gameSeqNo;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String historyId;

    @NotBlank
    @JsonProperty("gType")
    @Pattern(regexp = "^[0-9]+$")
    private String gType;

    @NotBlank
    @JsonProperty("mType")
    @Pattern(regexp = "^[0-9]+$")
    private String mType;

    @Size(max = 10)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4}$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String reportDate;

    @Size(max = 19)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String gameDate;

    @Size(max = 19)
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String lastModifyTime;

    @NotNull
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal bet;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal validBet;

    @NotNull
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal win;

    @NotNull
    private BigDecimal netWin;


    private BigDecimal tax;
    private String sessionNo;

    @Override
    public String getExternalTransactionId() {
        return refTransferIds.get(0).toString();
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
        // check provider's category to decide round id value
        if (this.getGType().equals(Formats.SPRIBE)) {
            return String.valueOf(refTransferIds.get(0));
        } else {
            return this.gameRoundSeqNo.toString();
        }
    }

    @Override
    public String getGameId() {
        return mType;
    }

    @Override
    public BigDecimal getBetAmount() {
        return bet;
    }

    @Override
    public BigDecimal getWinAmount() {
        return win;
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
