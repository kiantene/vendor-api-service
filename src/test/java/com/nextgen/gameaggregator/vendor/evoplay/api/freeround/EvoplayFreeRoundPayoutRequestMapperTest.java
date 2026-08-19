package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvoplayFreeRoundPayoutRequestMapperTest {
    private final EvoplayFreeRoundPayoutRequestMapper mapper = new EvoplayFreeRoundPayoutRequestMapper();

    @Test
    void should_map_evoplay_free_round_request_to_promo_payout_context() {
        EvoplayFreeRoundPayoutRequest request = EvoplayFreeRoundPayoutRequest.builder()
                .id("txn-1")
                .userId("vendor-player-1")
                .eventId("registry-1")
                .currency("USD")
                .amount(new BigDecimal("12.34"))
                .build();

        var context = mapper.toInternal(request);

        assertThat(context.getIdempotencyKey()).isEqualTo("txn-1");
        assertThat(context.getVendorTransactionId()).isEqualTo("txn-1");
        assertThat(context.getVendorPlayerUsername()).isEqualTo("vendor-player-1");
        assertThat(context.getVendorCampaignCode()).isEqualTo("registry-1");
        assertThat(context.getVendorCurrency()).isEqualTo("USD");
        assertThat(context.getVendorPayoutAmount()).isEqualByComparingTo("12.34");
        assertThat(context.getPromoType()).isEqualTo(PromoType.FREE_ROUND);
    }
}
