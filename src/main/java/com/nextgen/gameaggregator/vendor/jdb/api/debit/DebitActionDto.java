package com.nextgen.gameaggregator.vendor.jdb.api.debit;

import jakarta.validation.GroupSequence;
import com.nextgen.gameaggregator.vendor.jdb.api.AmountPrecisionCheck;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.Formats;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * JDB Action 9 wire payload — debit/place-bet.
 * <p>
 * Renamed from the legacy {@code BetDto} to align with the new debit/credit framework's
 * vocabulary (the same JDB Action 9 is modeled as a "debit" in the new framework, not a "bet").
 * The wire shape is unchanged; only the type name moved.
 * <p>
 * {@link BetResultData} is still implemented for compatibility with the legacy
 * {@code BetService} call site (which the new framework supersedes after dual-stack soak).
 */
@GroupSequence({DebitActionDto.class, AmountPrecisionCheck.class})
@Data
public class DebitActionDto implements BetResultData {
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

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private Long gameRoundSeqNo;

    @NotBlank
    @JsonProperty("gType")
    @Pattern(regexp = "^[0-9]+$")
    private String gType;

    @NotBlank
    @JsonProperty("mType")
    @Pattern(regexp = "^[0-9]+$")
    private String mType;

    private BigDecimal effectiveTurnover;

    /**
     * JDB's own session identifier, echoed on every wallet callback.
     * <p>
     * NOT a GA session token: GA neither issues nor persists it (the launch response is not
     * stored against the GameSession), so it can never resolve via {@code findByToken}. It must
     * therefore NOT be mapped onto {@code DebitContext.token} — doing so makes
     * {@code GameSessionDataService.getGameSession} take its token branch, which throws
     * {@code GameSessionExpiredException} on a miss rather than falling through to the
     * {@code vendorPlayerUsername} lookup, surfacing to JDB as 7501 "User ID cannot be found".
     * Kept on the DTO only because the mapper runs with FAIL_ON_UNKNOWN_PROPERTIES enabled.
     */
    @JsonProperty("systemSessionId")
    private String systemSessionId;

    @Override
    public String getExternalTransactionId() {
        return this.transferId.toString();
    }

    @Override
    public String getRoundId() {
        // trim the provider's category for decide round id value
        String trimCategory = this.mType.toString().toString().substring(0, 2);

        if(trimCategory.equals(Formats.SPRIBE)){
            return this.transferId.toString();
        }else{
            return this.gameRoundSeqNo.toString();
        }
    }

    @Override
    public String getGameId() {
        return this.mType.toString();
    }

    /**
     * Platform vendor game code. JDB splits a game code into {@code gType} (category) and
     * {@code mType} (game) on the wire, but persists and looks up the joined
     * {@code gType_mType} form — see {@code GameUrlService}, which splits the session's
     * {@code vendorGameCode} on "_" to build the launch request, and
     * {@code vendor_games.vendor_game_code} (e.g. "18_18025"). Mapping bare {@code mType}
     * here would miss every VendorGame row.
     */
    public String getVendorGameCode() {
        return this.gType + "_" + this.mType;
    }

    @Override
    public String getVendorBetId() {
        return this.transferId.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return effectiveTurnover;
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

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
