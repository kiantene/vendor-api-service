package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evolution v2 promo-payout integration.
 *
 * <p>Models {@code promoTransaction} from the Evolution {@code PromoPayoutRequest}. Fields are
 * populated selectively depending on {@code type} (see the Promo Transaction Types in the
 * One Wallet API spec), so all type-specific fields are nullable.</p>
 *
 * <p><b>Polymorphic on {@code type}:</b> {@code campaignId} differs in shape per vendor system, so
 * those types deserialize to a dedicated subtype — {@link RedTigerPromoTransactionDto} (numeric
 * {@code campaignId}) for {@code SmartTournamentMonetaryReward} / {@code SmartSpinsMonetaryReward},
 * and {@link LivespinsPromoTransactionDto} (string {@code campaignId}) for {@code CashReward}. All
 * other types deserialize to this base class. Use {@link #resolveCampaignId()} to read the
 * campaign id uniformly as a {@code String}.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type", visible = true, defaultImpl = PromoTransactionDto.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RedTigerPromoTransactionDto.class, name = "SmartTournamentMonetaryReward"),
        @JsonSubTypes.Type(value = RedTigerPromoTransactionDto.class, name = "SmartSpinsMonetaryReward"),
        @JsonSubTypes.Type(value = LivespinsPromoTransactionDto.class, name = "CashReward")
})
public class PromoTransactionDto {

    @NotBlank
    private String type;

    @NotBlank
    private String id;

    @NotNull
    @PositiveOrZero
    private BigDecimal amount;

    /** Voucher initialization / origination info. Present for free-round and Rtr types. */
    private OriginDto origin;

    // --- Free-round / reward-game types (FreeRoundPlayableSpent, RewardGame*) ---
    private String voucherId;
    private Integer remainingRounds;
    private BigDecimal playableBalance;

    // --- JackpotWin ---
    private List<JackpotDto> jackpots;

    // --- RtrMonetaryReward ---
    private String bonusConfigId;
    private String rewardId;

    // --- SmartTournamentMonetaryReward / SmartSpinsMonetaryReward ---
    private String instanceCode;
    private Integer instanceId;
    private String campaignCode;

    // --- CashReward ---
    private String reason;

    /**
     * Campaign id resolved as a {@code String} regardless of vendor representation — numeric for
     * Red Tiger (Smart Tournament/Spins), string for Livespins (Cash Reward). Returns {@code null}
     * for promo types that do not carry a campaign id. Overridden by the polymorphic subtypes.
     */
    public String resolveCampaignId() {
        return null;
    }
}
