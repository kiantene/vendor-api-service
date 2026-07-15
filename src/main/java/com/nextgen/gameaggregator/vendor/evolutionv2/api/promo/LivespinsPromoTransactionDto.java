package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Evolution v2 promo-payout integration.
 *
 * <p>Promo transaction subtype for the Livespins promo system — {@code CashReward}. Here
 * {@code campaignId} is a <b>string</b> identifier.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivespinsPromoTransactionDto extends PromoTransactionDto {

    /** Internal campaign identifier in the Livespins promo system (string). Optional. */
    private String campaignId;

    @Override
    public String resolveCampaignId() {
        return campaignId;
    }
}
