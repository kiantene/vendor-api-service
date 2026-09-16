package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Promo transaction details on {@code /transactions/promoWin}.
 *
 * <p>Mandatory in the Aviatrix spec. {@code bonusId} is present only when {@code type} is
 * {@code "bonus"}; a {@code "tournament"} payout carries no bonus reference.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromoDto {

    /** Promo transaction type. Possible values: {@code bonus}, {@code tournament}. */
    @NotBlank
    @Size(max = 50)
    private String type;

    /** Bonus identifier. Present when {@code type} is {@code bonus}. */
    @Size(max = 255)
    private String bonusId;
}
