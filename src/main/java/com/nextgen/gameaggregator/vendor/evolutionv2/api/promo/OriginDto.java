package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Evolution v2 promo-payout integration.
 *
 * <p>Voucher initialization / origination info ({@code promoTransaction.origin}). Present for
 * {@code FreeRoundPlayableSpent} (origin {@code "SpinGifts"}) and {@code RtrMonetaryReward}
 * (origin {@code "Tournament"}).</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OriginDto {

    /** Voucher origination source type, e.g. "SpinGifts" or "Tournament". */
    private String type;
}
