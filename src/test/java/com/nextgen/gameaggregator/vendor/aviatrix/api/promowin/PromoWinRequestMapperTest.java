package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoWinRequestMapperTest {

    private final PromoWinRequestMapper mapper = new PromoWinRequestMapper();

    @Test
    void mapsATournamentPayoutWithNoCampaignKey() {
        PromoPayoutContext context = mapper.toInternal(PromoWinDtos.of("tournament", null));

        assertThat(context.getPromoType()).isEqualTo(PromoType.TOURNAMENT);
        // No bonus id means no campaign to resolve, which the Operator spec allows for a
        // Vendor Network Tournament payout. The enricher skips resolution entirely.
        assertThat(context.getVendorFreeRoundBonusId()).isNull();
        assertThat(context.getVendorCampaignCode()).isNull();
    }

    /**
     * The bonus id is deliberately dropped. It fed a campaign lookup that could never match, and nothing
     * else consumes it — neither the operator request nor promo_payout_history has a field for a
     * vendor-side promo reference.
     */
    @Test
    void mapsABonusPayoutWithoutCarryingTheBonusId() {
        PromoPayoutContext context = mapper.toInternal(PromoWinDtos.of("bonus", "bonus-9"));

        // FREE_ROUND, not BONUS: BONUS is not in the Operator promoType contract and is forwarded verbatim
        assertThat(context.getPromoType()).isEqualTo(PromoType.FREE_ROUND);
        assertThat(context.getVendorFreeRoundBonusId()).isNull();
        assertThat(context.getVendorCampaignCode()).isNull();
    }

    @Test
    void mapsTheVendorFieldsOntoTheContext() {
        PromoPayoutContext context = mapper.toInternal(PromoWinDtos.of("tournament", null));

        assertThat(context.getIdempotencyKey()).isEqualTo("tx-1");
        assertThat(context.getVendorTransactionId()).isEqualTo("tx-1");
        assertThat(context.getVendorPlayerUsername()).isEqualTo("player-1");
        assertThat(context.getVendorSessionToken()).isEqualTo("token-1");
        assertThat(context.getVendorCurrency()).isEqualTo("USD");
        // 333 minor units -> 3.33 major
        assertThat(context.getVendorPayoutAmount()).isEqualByComparingTo("3.33");
    }


    @Test
    void rejectsAnUnrecognisedPromoType() {
        assertThatThrownBy(() -> mapper.toInternal(PromoWinDtos.of("jackpot", null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("jackpot");
    }

    @Test
    void rejectsAMissingPromoType() {
        assertThatThrownBy(() -> mapper.toInternal(PromoWinDtos.of(null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

}
