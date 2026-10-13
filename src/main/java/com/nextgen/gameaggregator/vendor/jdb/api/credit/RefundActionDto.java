package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import jakarta.validation.GroupSequence;
import com.nextgen.gameaggregator.vendor.jdb.api.AmountPrecisionCheck;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * JDB Action 11 wire payload — cancel/rollback.
 * <p>
 * Renamed from the legacy {@code CancelBetDto} to align with the new debit/credit framework's
 * vocabulary (Action 11 is routed through the credit path with {@code isRefund=true}).
 * The wire shape is unchanged; only the type name moved.
 */
@GroupSequence({RefundActionDto.class, AmountPrecisionCheck.class})
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundActionDto implements RollbackData {
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
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
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
    @Positive
    private Long gameRoundSeqNo;

    @NotBlank
    @JsonProperty("gType")
    @Pattern(regexp = "^[0-9]+$")
    private String gType;

    @NotBlank
    @JsonProperty("mType")
    @Pattern(regexp = "^[0-9]+$")
    private String mType;

    @JsonProperty("systemSessionId")
    private String systemSessionId;

    /**
     * Platform vendor game code — the joined {@code gType_mType} form that the GameSession and
     * {@code vendor_games.vendor_game_code} store. See {@code DebitActionDto#getVendorGameCode()}.
     */
    public String getVendorGameCode() {
        return this.gType + "_" + this.mType;
    }

    @Override
    public String getRollbackId() {
        if (refTransferIds != null && !refTransferIds.isEmpty()) {
            return String.valueOf(refTransferIds.get(0));
        }
        return null;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
