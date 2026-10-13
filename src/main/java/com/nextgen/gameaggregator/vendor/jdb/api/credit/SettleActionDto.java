package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import jakarta.validation.GroupSequence;
import com.nextgen.gameaggregator.vendor.jdb.api.AmountPrecisionCheck;
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

/**
 * JDB Action 10 wire payload — settle/credit.
 * <p>
 * Renamed from the legacy {@code SettleDto} to align with the new debit/credit framework's
 * vocabulary (the same JDB Action 10 is modeled as a "credit" in the new framework).
 * The wire shape is unchanged; only the type name moved.
 */
@GroupSequence({SettleActionDto.class, AmountPrecisionCheck.class})
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleActionDto implements BetResultData {
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
    // JDB publishes 4-character currency codes alongside the 2- and 3-character ones
    // (IRRK, KHRO, KRWK, VNDC, DOGE, mBTC, USDC, POGC, POSC), so max=3 rejected them.
    @Size(max = 4)
    private String currency;

    @NotNull
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    // JDB amounts carry at most 6 decimal places (and 20 integer digits); more is a malformed
    // request, not a value to round. No message of its own, so it surfaces as 8000 (OVI-2162 / 2291). Checked after the
    // default group (see AmountPrecisionCheck), so a negative amount still answers 8003.
    @Digits(integer = 20, fraction = 6, groups = AmountPrecisionCheck.class)
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

    // Date fields carry @Pattern only. The pattern already fixes the exact length, and a second
    // constraint on the same field made a malformed value answer 8000 or 8005 at random:
    // ValidationUtils keeps one violation per field, and the validator returns them unordered
    // (OVI-3191). A malformed date of any length is 8005.
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4}$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String reportDate;

    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String gameDate;

    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-\\d{4} (?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = ResponseCode.WRONG_DATE_FORMAT)
    private String lastModifyTime;

    @NotNull
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    @Digits(integer = 20, fraction = 6, groups = AmountPrecisionCheck.class)
    private BigDecimal bet;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    @Digits(integer = 20, fraction = 6, groups = AmountPrecisionCheck.class)
    private BigDecimal validBet;

    @NotNull
    @PositiveOrZero(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    @Digits(integer = 20, fraction = 6, groups = AmountPrecisionCheck.class)
    private BigDecimal win;

    @NotNull
    @Digits(integer = 20, fraction = 6, groups = AmountPrecisionCheck.class)
    private BigDecimal netWin;


    private BigDecimal tax;
    private String sessionNo;

    @JsonProperty("systemSessionId")
    private String systemSessionId;

    /**
     * JDB Action 10 round-closure flag. Per JDB API doc, true when this credit closes
     * the round (no further legs expected). Nullable — JDB may omit it on intermediate
     * legs in multi-leg rounds. Read verbatim by {@code JdbSettleContextMapper} into
     * {@code CreditContext.isRoundEnded}; the framework's
     * {@code WalletRoundLedgerDataService.markSettled} fires only when this is true.
     */
    @JsonProperty("roundClosed")
    private Boolean roundClosed;

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

    /**
     * Platform vendor game code — the joined {@code gType_mType} form that the GameSession and
     * {@code vendor_games.vendor_game_code} store. See {@code DebitActionDto#getVendorGameCode()}.
     */
    public String getVendorGameCode() {
        return this.gType + "_" + this.mType;
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

    /**
     * {@code ts}, like {@link #getVendorBetTime()}: bet time and settle time both come from the
     * request's epoch-millis {@code ts} (OVI-2164). {@code gameDate} is a GMT-4 wall-clock string
     * whose parse failures silently fell back to {@code ts} anyway; it is still validated for
     * format ({@code 8005}) but no longer read for time.
     */
    @Override
    public Long getResultTime() {
        return ts;
    }

    /** {@code ts}; see {@link #getResultTime()}. */
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

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
