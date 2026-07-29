package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Evolution v2 promo-payout integration.
 *
 * <p>A single winning jackpot payoff within {@code promoTransaction.jackpots}. Present when the
 * promo transaction type is {@code JackpotWin}.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JackpotDto {

    /** ID of the winning jackpot. */
    private String id;

    /** Win amount of the jackpot in the player's session currency. */
    private BigDecimal winAmount;
}
