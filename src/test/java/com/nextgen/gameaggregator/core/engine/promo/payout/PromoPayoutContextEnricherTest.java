package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.core.service.data.CampaignDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromoPayoutContextEnricherTest {

    @Mock private AgentPlayerDataService agentPlayerDataService;
    @Mock private VendorPlayerDataService vendorPlayerDataService;
    @Mock private VendorGameDataService vendorGameDataService;
    @Mock private CurrencyDataService currencyDataService;
    @Mock private VendorCurrencyDataService vendorCurrencyDataService;
    @Mock private VendorDataService vendorDataService;
    @Mock private AgentDataService agentDataService;
    @Mock private CampaignDataService campaignDataService;

    @Mock private Agent stubAgent;
    @Mock private Vendor stubVendor;

    private PromoPayoutContextEnricher enricher;

    private static final long REQUEST_RECEIVE_TIME = 1_700_000_000_000L;

    @BeforeEach
    void setUp() {
        enricher = new PromoPayoutContextEnricher(
                agentPlayerDataService, vendorPlayerDataService, vendorGameDataService,
                currencyDataService, vendorCurrencyDataService,
                vendorDataService, agentDataService, campaignDataService);

        lenient().when(agentDataService.get(any())).thenReturn(stubAgent);
        lenient().when(vendorDataService.get(any())).thenReturn(stubVendor);
    }

    private PromoPayoutContext baseContext() {
        PromoPayoutContext ctx = PromoPayoutContext.builder().build();
        ctx.getAgent().id(1);
        ctx.getVendor().id(1);
        return ctx;
    }

    // -----------------------------------------------------------------------
    // Single-transaction mode
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("vendorTransactionTime fallback — single mode")
    class SingleMode {

        @Test
        @DisplayName("null vendorTransactionTime is set from logContext.getStart() (request receive time)")
        void nullTime_isFilledFromRequestReceiveTime() {
            PromoPayoutContext ctx = baseContext();

            // Stub the LogContext before entering the static-mock scope to avoid nested when() calls
            LogContext lc = mock(LogContext.class);
            when(lc.getStart()).thenReturn(REQUEST_RECEIVE_TIME);
            when(lc.getTraceId()).thenReturn("trace-test-001");

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                enricher.doEnrich(ctx);
            }

            assertThat(ctx.getVendorTransactionTime()).isEqualTo(REQUEST_RECEIVE_TIME);
        }

        @Test
        @DisplayName("pre-set vendorTransactionTime is not overwritten")
        void preSetTime_isPreserved() {
            long vendorTime = 12345L;
            PromoPayoutContext ctx = baseContext();
            ctx.setVendorTransactionTime(vendorTime);

            LogContext lc = mock(LogContext.class);
            when(lc.getStart()).thenReturn(REQUEST_RECEIVE_TIME);
            when(lc.getTraceId()).thenReturn("trace-test-001");

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                enricher.doEnrich(ctx);
            }

            assertThat(ctx.getVendorTransactionTime()).isEqualTo(vendorTime);
        }
    }

    // -----------------------------------------------------------------------
    // Batch-transaction mode (context.payoutTransactions list)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("vendorTransactionTime fallback — batch mode")
    class BatchMode {

        @Test
        @DisplayName("null vendorTransactionTime in each child transaction is set from logContext.getStart()")
        void nullTimeInBatchTxns_isFilledFromRequestReceiveTime() {
            PayoutTransaction txn1 = PayoutTransaction.builder().build();
            PayoutTransaction txn2 = PayoutTransaction.builder().build();
            PromoPayoutContext ctx = baseContext();
            ctx.setPayoutTransactions(List.of(txn1, txn2));

            LogContext lc = mock(LogContext.class);
            when(lc.getStart()).thenReturn(REQUEST_RECEIVE_TIME);
            when(lc.getTraceId()).thenReturn("trace-test-001");

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                enricher.doEnrich(ctx);
            }

            assertThat(txn1.getVendorTransactionTime()).isEqualTo(REQUEST_RECEIVE_TIME);
            assertThat(txn2.getVendorTransactionTime()).isEqualTo(REQUEST_RECEIVE_TIME);
        }

        @Test
        @DisplayName("pre-set vendorTransactionTime in child transactions is not overwritten")
        void preSetTimeInBatchTxns_isPreserved() {
            long txn1Time = 11111L;
            long txn2Time = 22222L;
            PayoutTransaction txn1 = PayoutTransaction.builder().vendorTransactionTime(txn1Time).build();
            PayoutTransaction txn2 = PayoutTransaction.builder().vendorTransactionTime(txn2Time).build();
            PromoPayoutContext ctx = baseContext();
            ctx.setPayoutTransactions(List.of(txn1, txn2));

            LogContext lc = mock(LogContext.class);
            when(lc.getStart()).thenReturn(REQUEST_RECEIVE_TIME);
            when(lc.getTraceId()).thenReturn("trace-test-001");

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                enricher.doEnrich(ctx);
            }

            assertThat(txn1.getVendorTransactionTime()).isEqualTo(txn1Time);
            assertThat(txn2.getVendorTransactionTime()).isEqualTo(txn2Time);
        }
    }
}
