package com.nextgen.gameaggregator.core.engine.promo.campaign;

/**
 * Strategy sent to the promo-engine's {@code /internal/resolveCampaign} endpoint, telling it how to
 * resolve the campaign and which keys the request {@code params} map carries.
 *
 * <p><b>Wire contract:</b> these enum names are serialized as-is and must match the promo-engine's
 * own {@code CampaignResolveStrategy} enum exactly. Whenever a new strategy is added to either side,
 * the other side must be updated in the same release. Adding a value here without a matching update
 * on the promo-engine side (or vice versa) will cause a runtime deserialization error.
 *
 * <p>See also: {@code ga-promo-engine} project, {@code CampaignResolveStrategy}.
 */
public enum CampaignResolveStrategy {
    /** params: {@code vendorLineId}, {@code vendorCampaignCode}, optional {@code campaignType}. */
    VENDOR_LINE_AND_CODE,
    /** params: {@code playerUuid}. */
    PLAYER_UUID,
    /** params: {@code username}, {@code freeRoundBonusId}. */
    USERNAME_AND_BONUS_ID
}
