package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FreeSpinResultDto extends CommonDto {

    @NotNull
    @PositiveOrZero(message = ResponseCodes.INVALID_AMOUNT)
    @Digits(integer = 20, fraction = 8, message = ResponseCodes.INVALID_AMOUNT)
    public BigDecimal amount;

    /**
     * The grant these rounds came from — {@code freespin_id} as minted by {@code createFreeSpin} and
     * recorded per player as {@code CampaignPlayer.ext_info.vendorGrantRef} in the promo engine.
     *
     * <p>Required: it is the only field on this callback that identifies which campaign to attribute the
     * win to. Without it the payout cannot be resolved, so rejecting with {@code 5001} is preferable to
     * crediting an unattributed amount.
     */
    @NotNull
    public Long freespinId;

    @NotBlank
    @Size(max = 255)
    public String gameId;

    @NotBlank
    @Size(max = 255)
    public String gameName;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\S]+$") // not allow whitespace
    public String roundId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\S]+$") // not allow whitespace
    public String wagerId;

    @NotBlank
    @Size(max = 255)
    public String provider;

    /** ISO-8601 instant, e.g. {@code 2026-08-11T09:11:17Z}. Optional — absent falls back to receive time. */
    public String transactionTime;

    /**
     * JSON boolean ({@code "is_endround":true}), held as a String because Jackson coerces it.
     *
     * <p><b>Deliberately unused.</b> Every {@code freeSpinResult} observed in STG carries {@code true} —
     * {@code false} appears only on {@code /wager}. That matches the shape of the API: DCS splits a
     * multi-part paid round across {@code wager} / {@code appendWager} / {@code endWager}, and there is
     * no {@code appendFreeSpin}, so a free spin has nothing to settle in parts. Each callback is a whole
     * round, and each is credited.
     *
     * <p>Kept on the DTO rather than dropped so the mapper can warn if that ever stops being true — if
     * DCS began sending non-terminal partials, crediting every one would over-pay the round.
     */
    public String isEndround;

    /**
     * {@code transaction_time} as epoch millis, or null when absent or unparseable.
     *
     * <p>Null is deliberate rather than a {@code now()} fallback: the payout enricher already substitutes
     * the request receive time, which is a better approximation than a value produced further downstream.
     * Parsed with {@link Instant#parse} rather than {@code DateTimeConversionUtils}, whose defaults are
     * GMT+8 and a space-separated pattern — both wrong for this {@code Z}-suffixed field.
     */
    public Long getTransactionTimeMillis() {
        if (transactionTime == null || transactionTime.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(transactionTime).toEpochMilli();
        } catch (Exception e) {
            log.warn("Unparseable transaction_time '{}' on freeSpinResult, falling back to receive time", transactionTime);
            return null;
        }
    }
}
