package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.tournament;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentPayoutRequestMapperTest {

    private final TournamentPayoutRequestMapper mapper = new TournamentPayoutRequestMapper();

    @Test
    void toInternal_mapsTournamentPayoutRequest() {
        TournamentPayoutRequest vendorRequest = new TournamentPayoutRequest();
        vendorRequest.setProviderId("provider-transaction-id");
        vendorRequest.setReference("reference123");
        vendorRequest.setUserId("player123");
        vendorRequest.setCampaignId("campaign-123");
        vendorRequest.setAmount(new BigDecimal("10.50"));
        vendorRequest.setTimestamp(1754373936436L);

        PromoPayoutContext context = mapper.toInternal(vendorRequest);

        assertThat(context.getIdempotencyKey()).isEqualTo("reference123");
        assertThat(context.getVendorPlayerUsername()).isEqualTo("player123");
        assertThat(context.getVendorCampaignCode()).isEqualTo("campaign-123");
        assertThat(context.getVendorTransactionId()).isEqualTo("reference123");
        assertThat(context.getVendorPayoutAmount()).isEqualByComparingTo("10.50");
        assertThat(context.getVendorTransactionTime()).isEqualTo(1754373936436L);
        assertThat(context.getPromoType()).isEqualTo(PromoType.TOURNAMENT);
    }
}
