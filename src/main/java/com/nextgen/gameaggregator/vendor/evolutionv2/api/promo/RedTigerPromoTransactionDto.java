package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Evolution v2 promo-payout integration.
 *
 * <p>Promo transaction subtype for the Red Tiger promo system — {@code SmartTournamentMonetaryReward}
 * and {@code SmartSpinsMonetaryReward}. Here {@code campaignId} is an internal <b>numeric</b> identifier.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedTigerPromoTransactionDto extends PromoTransactionDto {

    /** Internal campaign identifier in the Red Tiger promo system (numeric). Optional. */
    private Integer campaignId;

    @Override
    public String resolveCampaignId() {
        return campaignId == null ? null : String.valueOf(campaignId);
    }
}
