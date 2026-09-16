package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutDto;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutMapper;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What Aviatrix's {@code promo.type} becomes in the body sent to the operator's {@code /v1/promo/payout}.
 *
 * <p>{@code PromoPayoutMapperTest} covers that every {@code PromoType} is forwarded as its code, and
 * {@code PromoWinRequestMapperTest} covers the vendor string to {@code PromoType} step. Neither joins
 * them, so nothing proved an Aviatrix payload arrives at the operator correctly classified.
 */
class PromoWinOperatorPayloadTest {

    private final PromoPayoutMapper operatorMapper = new PromoPayoutMapper();

    @ParameterizedTest
    @CsvSource({
            "bonus,      bonus-9, FREEROUND",
            "tournament, ,        TOURNAMENT"
    })
    void aviatrixPromoTypeReachesTheOperatorBody(String vendorType, String bonusId, String expectedCode)
            throws Exception {

        PromoPayoutContext context = new PromoWinRequestMapper()
                .toInternal(PromoWinDtos.of(vendorType, bonusId));
        simulateEnrichment(context);

        PromoPayoutDto operatorRequest = operatorMapper.toPromoPayoutRequest(context);

        assertThat(operatorRequest.getPromoType()).isEqualTo(expectedCode);
        assertThat(new ObjectMapper().writeValueAsString(operatorRequest))
                .contains("\"promoType\":\"" + expectedCode + "\"");
    }

    /**
     * A tournament resolves no campaign, so {@code campaignId} is omitted by {@code JsonInclude.NON_NULL}
     * and {@code promoType} is the operator's only description of what the credit is — the case the
     * {@code PromoPayoutDto.promoType} contract calls out explicitly.
     */
    @Test
    void aTournamentIsClassifiedEvenWithNoCampaign() throws Exception {
        PromoPayoutContext context = new PromoWinRequestMapper()
                .toInternal(PromoWinDtos.of("tournament", null));
        simulateEnrichment(context);

        String json = new ObjectMapper().writeValueAsString(operatorMapper.toPromoPayoutRequest(context));

        assertThat(json).doesNotContain("campaignId");
        assertThat(json).contains("\"promoType\":\"TOURNAMENT\"");
        assertThat(json).contains("\"amount\":3.33");
    }

    @Test
    void aBonusCarriesBothItsCampaignAndItsType() throws Exception {
        PromoPayoutContext context = new PromoWinRequestMapper()
                .toInternal(PromoWinDtos.of("bonus", "bonus-9"));
        simulateEnrichment(context);
        context.setCampaignUuid("11111111-2222-3333-4444-555555555555");

        String json = new ObjectMapper().writeValueAsString(operatorMapper.toPromoPayoutRequest(context));

        assertThat(json).contains("\"campaignId\":\"11111111-2222-3333-4444-555555555555\"");
        assertThat(json).contains("\"promoType\":\"FREEROUND\"");
    }

    /** Stands in for PromoPayoutContextEnricher, setting only what PromoPayoutMapper reads. */
    private static void simulateEnrichment(PromoPayoutContext context) {
        context.setTraceId("trace-1");
        context.getAgent().playerUsername("agent-player-1");
        context.setTransactionId("0198c0ffee00700000000000000000ab");
        context.setCurrencyCode("USD");
        context.setVendorTransactionTime(1755000000000L);
        context.setPayout(TxnAmount.of(context.getVendorPayoutAmount(), BigDecimal.ONE));
    }
}
