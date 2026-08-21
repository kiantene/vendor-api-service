package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class EvoplayFreeRoundPayoutServiceTest {

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @Test
    void should_return_promo_payout_result_when_service_succeeds() {
        PromoPayoutService promoPayoutService = fluentPromoPayoutService();
        when(promoPayoutService.process(any()))
                .thenReturn(new PlayerBalanceData("player-1", "USD", new BigDecimal("88.88"), 1L));

        EvoplayFreeRoundPayoutService service = service(promoPayoutService);

        EvoplayFreeRoundPayoutResult result = service.payout(request(), httpRequestLog());

        assertThat(result.getBalance()).isEqualByComparingTo("88.88");
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void should_return_remembered_balance_for_duplicate_request() {
        RequestIdempotentLog duplicateLog = new RequestIdempotentLog();
        duplicateLog.setBalance(new BigDecimal("77.77"));
        duplicateLog.setCurrency("CNY");

        PromoPayoutService promoPayoutService = fluentPromoPayoutService();
        when(promoPayoutService.process(any()))
                .thenThrow(new DuplicateRequestException("duplicate", duplicateLog));

        EvoplayFreeRoundPayoutService service = service(promoPayoutService);

        EvoplayFreeRoundPayoutResult result = service.payout(request(), httpRequestLog());

        assertThat(result.getBalance()).isEqualByComparingTo("77.77");
        assertThat(result.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void should_swallow_promo_exception_and_return_default_balance() {
        PromoPayoutService promoPayoutService = fluentPromoPayoutService();
        when(promoPayoutService.process(any()))
                .thenThrow(new RuntimeException("operator unavailable"));

        EvoplayFreeRoundPayoutService service = service(promoPayoutService);

        EvoplayFreeRoundPayoutResult result = service.payout(request(), httpRequestLog());

        assertThat(result.getBalance()).isZero();
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void should_reject_negative_vendor_amount_before_processing_payout() {
        PromoPayoutService promoPayoutService = fluentPromoPayoutService();
        EvoplayFreeRoundPayoutService service = service(promoPayoutService);

        assertThatThrownBy(() -> service.payout(request("-0.01"), httpRequestLog()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive or zero");

        verify(promoPayoutService, never()).process(any());
    }

    @Test
    void should_enable_player_uuid_campaign_lookup_when_request_requires_it() {
        PromoPayoutService promoPayoutService = fluentPromoPayoutService();
        when(promoPayoutService.process(any()))
                .thenReturn(new PlayerBalanceData("player-1", "USD", new BigDecimal("88.88"), 1L));

        EvoplayFreeRoundPayoutService service = service(promoPayoutService);

        service.payout(request(true), httpRequestLog());

        @SuppressWarnings("unchecked")
        var configurerCaptor = org.mockito.ArgumentCaptor.forClass(Consumer.class);
        verify(promoPayoutService).configure(configurerCaptor.capture());

        PromoPayoutConfig config = new PromoPayoutConfig();
        ((Consumer<PromoPayoutConfig>) configurerCaptor.getValue()).accept(config);
        assertThat(config.isPlayerUuidCampaignLookup()).isTrue();
    }

    private EvoplayFreeRoundPayoutService service(PromoPayoutService promoPayoutService) {
        return new EvoplayFreeRoundPayoutService(
                new EvoplayFreeRoundPayoutRequestMapper(),
                new EvoplayFreeRoundPayoutResponseMapper(),
                promoPayoutService
        );
    }

    private PromoPayoutService fluentPromoPayoutService() {
        PromoPayoutService promoPayoutService = mock(PromoPayoutService.class);
        when(promoPayoutService.initialise(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.configure(any())).thenReturn(promoPayoutService);
        return promoPayoutService;
    }

    private EvoplayFreeRoundPayoutRequest request() {
        return request("10.00", false);
    }

    private EvoplayFreeRoundPayoutRequest request(boolean playerUuidCampaignLookup) {
        return request("10.00", playerUuidCampaignLookup);
    }

    private EvoplayFreeRoundPayoutRequest request(String amount) {
        return request(amount, false);
    }

    private EvoplayFreeRoundPayoutRequest request(String amount, boolean playerUuidCampaignLookup) {
        return EvoplayFreeRoundPayoutRequest.builder()
                .id("txn-1")
                .userId("vendor-player-1")
                .eventId("registry-1")
                .currency("USD")
                .amount(new BigDecimal(amount))
                .playerUuidCampaignLookup(playerUuidCampaignLookup)
                .build();
    }

    private HttpRequestLog httpRequestLog() {
        HttpRequestLog log = new HttpRequestLog();
        log.setId("trace-1");
        return log;
    }
}
