package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

/**
 * Internal promo classification. {@link #code} is forwarded to operators on the promo payout request;
 * {@link #id} is stored in {@code promo_payout_history.promo_type}.
 *
 * <p><b>Wire contract:</b> {@link #id} is also sent to {@code ga-promo-engine} as {@code campaignType}
 * on the default campaign-resolution path, and that service's own {@code CampaignType} recognises only
 * 1 and 2. A vendor resolving on the default path must therefore not be assigned a value above 2 — the
 * lookup would match no campaign and the payout would fail. Values above 2 are usable only by vendors
 * resolving via {@code PLAYER_UUID} or {@code USERNAME_AND_BONUS_ID}, which never send
 * {@code campaignType}.
 *
 * <p>Ids and names follow the PRD's Campaign Type ordering. {@link #MISSION} and {@link #ONEAPI_JACKPOT}
 * have no producer yet — no vendor sends anything that maps to them — so they are declared to keep the
 * numbering aligned with the PRD, not because anything emits them. Do not map a vendor type onto either
 * without confirming its semantics first.
 *
 * <p>See also: {@code ga-promo-engine} project, {@code CampaignType}.
 */
@AllArgsConstructor
public enum PromoType {
    FREE_ROUND(1, "FREEROUND", "Free Round"),
    TOURNAMENT(2, "TOURNAMENT", "Tournament"),
    PRIZEDROP (3, "PRIZEDROP", "Prize Drop"),
    MISSION   (4, "MISSION", "Mission"),
    BONUS     (5, "BONUS", "Bonus"),
    JACKPOT   (6, "JACKPOT", "Jackpot"),
    ONEAPI_JACKPOT(7, "ONEAPIJACKPOT", "OneAPI Jackpot");

    public final Integer id;
    public final String code;
    public final String description;

}
