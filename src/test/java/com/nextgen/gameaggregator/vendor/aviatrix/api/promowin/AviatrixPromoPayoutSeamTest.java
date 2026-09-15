package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextEnricher;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextHolder;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutProcessor;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutServiceImpl;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutValidator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.idempotency.RequestIdempotencyService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * What the enricher actually sees, through the real {@link PromoPayoutContextHolder} ThreadLocal.
 *
 * <p><b>Aviatrix resolves no campaign, for either promo type.</b> OneAPI has no Aviatrix promo
 * integration — no campaign is created and no grant provisioned — so there is nothing for a payout to
 * point at. {@code USERNAME_AND_BONUS_ID} was configured for bonuses at one point and could never match:
 * it compares {@code freeRoundBonusId} against {@code CampaignPlayer.vendorGrantRef}, which only ever
 * holds ids a vendor handed back at provisioning time. Aviatrix mints {@code promo.bonusId} itself.
 *
 * <p>The failure was invisible — a failed resolve becomes an empty {@code Campaign}, not an error — so
 * these assertions exist to keep it from being reintroduced. A strategy or a {@code vendorCampaignCode}
 * appearing here would resume calling the promo engine on every payout for a lookup that cannot succeed.
 */
@ExtendWith(MockitoExtension.class)
class AviatrixPromoPayoutSeamTest {

    @Mock private DuplicateRequestGuard guard;
    @Mock private PromoPayoutContextEnricher enricher;
    @Mock private PromoPayoutValidator validator;
    @Mock private PromoPayoutProcessor processor;
    @Mock private RequestIdempotencyService requestIdempotencyService;

    @ParameterizedTest
    @CsvSource({
            "bonus,      bonus-9, FREE_ROUND",
            "tournament, ,        TOURNAMENT"
    })
    void noPromoTypeResolvesACampaign(String vendorType, String bonusId, PromoType expectedPromoType) {
        AtomicReference<PromoPayoutConfig> configAtEnrichTime = new AtomicReference<>();
        AtomicReference<PromoPayoutContext> contextAtEnrichTime = new AtomicReference<>();

        doAnswer(invocation -> {
            configAtEnrichTime.set(PromoPayoutContextHolder.getConfig());
            contextAtEnrichTime.set(invocation.getArgument(0, PromoPayoutContext.class));
            return null;
        }).when(enricher).enrich(any());

        when(processor.process(any())).thenReturn(
                PlayerBalanceData.getDefaultWithBalance("player-1", "USD", new BigDecimal("50.00")));

        service().payout(PromoWinDtos.of(vendorType, bonusId), httpRequestLog());

        // Both null together is what makes populateCampaign return before calling the promo engine.
        assertThat(configAtEnrichTime.get().getCampaignResolveStrategy()).isNull();
        assertThat(configAtEnrichTime.get().isPlayerUuidCampaignLookup()).isFalse();
        assertThat(contextAtEnrichTime.get().getVendorCampaignCode()).isNull();

        assertThat(contextAtEnrichTime.get().getPromoType()).isEqualTo(expectedPromoType);
    }

    /**
     * Batch returns success with a zero balance before the payout is processed; promoWin must report the
     * real balance, which the Aviatrix spec makes mandatory on a 200.
     */
    @Test
    void neverRunsABatchPayout() {
        AtomicReference<Boolean> batchAtEnrichTime = new AtomicReference<>();
        doAnswer(invocation -> {
            batchAtEnrichTime.set(PromoPayoutContextHolder.getConfig().isBatch());
            return null;
        }).when(enricher).enrich(any());
        when(processor.process(any())).thenReturn(
                PlayerBalanceData.getDefaultWithBalance("player-1", "USD", new BigDecimal("50.00")));

        service().payout(PromoWinDtos.of("bonus", "bonus-9"), httpRequestLog());

        assertThat(batchAtEnrichTime.get()).isFalse();
    }

    /** The holder is a ThreadLocal — a leak would carry one request's config into the next. */
    @Test
    void theHolderIsClearedAfterThePayout() {
        when(processor.process(any())).thenReturn(
                PlayerBalanceData.getDefaultWithBalance("player-1", "USD", new BigDecimal("50.00")));

        service().payout(PromoWinDtos.of("bonus", "bonus-9"), httpRequestLog());

        assertThat(PromoPayoutContextHolder.isInitialized()).isFalse();
    }

    private AviatrixPromoPayoutService service() {
        PromoPayoutServiceImpl engine = new PromoPayoutServiceImpl(
                guard, enricher, validator, processor, requestIdempotencyService);
        return new AviatrixPromoPayoutService(
                new PromoWinRequestMapper(), new PromoWinResponseMapper(), engine);
    }

    private static HttpRequestLog httpRequestLog() {
        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        return httpRequestLog;
    }
}
