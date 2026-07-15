package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignResolveStrategy;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.core.service.data.CampaignDataService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class);
                 MockedStatic<PromoPayoutContextHolder> cfgHolder = mockStatic(PromoPayoutContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                cfgHolder.when(PromoPayoutContextHolder::getConfig).thenReturn(new PromoPayoutConfig());
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

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class);
                 MockedStatic<PromoPayoutContextHolder> cfgHolder = mockStatic(PromoPayoutContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                cfgHolder.when(PromoPayoutContextHolder::getConfig).thenReturn(new PromoPayoutConfig());
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

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class);
                 MockedStatic<PromoPayoutContextHolder> cfgHolder = mockStatic(PromoPayoutContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                cfgHolder.when(PromoPayoutContextHolder::getConfig).thenReturn(new PromoPayoutConfig());
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

            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class);
                 MockedStatic<PromoPayoutContextHolder> cfgHolder = mockStatic(PromoPayoutContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                cfgHolder.when(PromoPayoutContextHolder::getConfig).thenReturn(new PromoPayoutConfig());
                enricher.doEnrich(ctx);
            }

            assertThat(txn1.getVendorTransactionTime()).isEqualTo(txn1Time);
            assertThat(txn2.getVendorTransactionTime()).isEqualTo(txn2Time);
        }
    }

    // -----------------------------------------------------------------------
    // Campaign resolution dispatch (strategy vs legacy paths + guard)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("campaign resolution dispatch")
    class CampaignResolution {

        @Test
        @DisplayName("strategy resolves via getByRef with username + freeRoundBonusId params")
        void strategy_resolvesViaGetByRef() {
            PromoPayoutContext ctx = baseContext();
            ctx.setVendorPlayerUsername("user-1");
            ctx.setVendorFreeRoundBonusId("frb-1");   // vendorCampaignCode left null, like Groove

            Campaign campaign = Campaign.builder().uuid("c-uuid").campaignName("FR").build();
            when(campaignDataService.getByRef(eq(CampaignResolveStrategy.USERNAME_AND_BONUS_ID), any()))
                    .thenReturn(campaign);

            runEnrich(ctx, new PromoPayoutConfig().campaignResolveStrategy(CampaignResolveStrategy.USERNAME_AND_BONUS_ID));

            verify(campaignDataService).getByRef(CampaignResolveStrategy.USERNAME_AND_BONUS_ID,
                    Map.of("username", "user-1", "freeRoundBonusId", "frb-1"));
            assertThat(ctx.getCampaignUuid()).isEqualTo("c-uuid");
            assertThat(ctx.getVendorCampaignName()).isEqualTo("FR");
        }

        @Test
        @DisplayName("strategy set but required params missing -> IllegalStateException")
        void strategy_missingParams_throws() {
            PromoPayoutContext ctx = baseContext();
            ctx.setVendorPlayerUsername("user-1");   // freeRoundBonusId intentionally null

            assertThatThrownBy(() -> runEnrich(ctx,
                    new PromoPayoutConfig().campaignResolveStrategy(CampaignResolveStrategy.USERNAME_AND_BONUS_ID)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("no strategy and no vendorCampaignCode -> resolution skipped (guard)")
        void noStrategyNoCode_skipsResolution() {
            PromoPayoutContext ctx = baseContext();   // vendorCampaignCode null, no strategy

            runEnrich(ctx, new PromoPayoutConfig());

            verify(campaignDataService, never()).getByRef(any(), any());
            verify(campaignDataService, never()).getByPlayerUuid(any());
            verify(campaignDataService, never()).get(any(), any(), any());
            assertThat(ctx.getCampaignUuid()).isNull();
        }

        @Test
        @DisplayName("PLAYER_UUID strategy: params map contains playerUuid from vendorCampaignCode")
        void playerUuid_strategy_resolvesViaGetByRef() {
            PromoPayoutContext ctx = baseContext();
            ctx.setVendorCampaignCode("player-uuid-42");

            Campaign campaign = Campaign.builder().uuid("c-42").campaignName("PU Campaign").build();
            when(campaignDataService.getByRef(eq(CampaignResolveStrategy.PLAYER_UUID), any()))
                    .thenReturn(campaign);

            runEnrich(ctx, new PromoPayoutConfig().campaignResolveStrategy(CampaignResolveStrategy.PLAYER_UUID));

            verify(campaignDataService).getByRef(CampaignResolveStrategy.PLAYER_UUID,
                    Map.of("playerUuid", "player-uuid-42"));
            assertThat(ctx.getCampaignUuid()).isEqualTo("c-42");
            assertThat(ctx.getVendorCampaignName()).isEqualTo("PU Campaign");
        }

        @Test
        @DisplayName("VENDOR_LINE_AND_CODE strategy: params map contains vendorLineId + vendorCampaignCode")
        void vendorLineAndCode_strategy_noPromoType_resolvesViaGetByRef() {
            PromoPayoutContext ctx = baseContext();
            ctx.getVendor().lineId(7);
            ctx.setVendorCampaignCode("VND-CAMPAIGN-X");

            Campaign campaign = Campaign.builder().uuid("c-vlc").campaignName("VLC Campaign").build();
            when(campaignDataService.getByRef(eq(CampaignResolveStrategy.VENDOR_LINE_AND_CODE), any()))
                    .thenReturn(campaign);

            runEnrich(ctx, new PromoPayoutConfig().campaignResolveStrategy(CampaignResolveStrategy.VENDOR_LINE_AND_CODE));

            verify(campaignDataService).getByRef(eq(CampaignResolveStrategy.VENDOR_LINE_AND_CODE),
                    argThat(params -> "7".equals(params.get("vendorLineId"))
                            && "VND-CAMPAIGN-X".equals(params.get("vendorCampaignCode"))
                            && !params.containsKey("campaignType")));
            assertThat(ctx.getCampaignUuid()).isEqualTo("c-vlc");
        }

        @Test
        @DisplayName("VENDOR_LINE_AND_CODE strategy: promoType included in params when present")
        void vendorLineAndCode_strategy_withPromoType_includesCampaignType() {
            PromoPayoutContext ctx = baseContext();
            ctx.getVendor().lineId(3);
            ctx.setVendorCampaignCode("VND-CAMPAIGN-Y");
            ctx.setPromoType(PromoType.FREE_ROUND);

            Campaign campaign = Campaign.builder().uuid("c-vlc2").build();
            when(campaignDataService.getByRef(eq(CampaignResolveStrategy.VENDOR_LINE_AND_CODE), any()))
                    .thenReturn(campaign);

            runEnrich(ctx, new PromoPayoutConfig().campaignResolveStrategy(CampaignResolveStrategy.VENDOR_LINE_AND_CODE));

            verify(campaignDataService).getByRef(eq(CampaignResolveStrategy.VENDOR_LINE_AND_CODE),
                    argThat(params -> "3".equals(params.get("vendorLineId"))
                            && "VND-CAMPAIGN-Y".equals(params.get("vendorCampaignCode"))
                            && "1".equals(params.get("campaignType"))));
        }

        @Test
        @DisplayName("legacy playerUuid lookup still routes to getByPlayerUuid (regression)")
        void legacyPlayerUuid_routesToGetByPlayerUuid() {
            PromoPayoutContext ctx = baseContext();
            ctx.setVendorCampaignCode("player-uuid-9");
            Campaign campaign = Campaign.builder().uuid("c-9").build();
            when(campaignDataService.getByPlayerUuid("player-uuid-9")).thenReturn(campaign);

            runEnrich(ctx, new PromoPayoutConfig().playerUuidCampaignLookup(true));

            verify(campaignDataService).getByPlayerUuid("player-uuid-9");
            verify(campaignDataService, never()).getByRef(any(), any());
            assertThat(ctx.getCampaignUuid()).isEqualTo("c-9");
        }

        @Test
        @DisplayName("PLAYER_UUID strategy with null vendorCampaignCode -> IllegalStateException")
        void playerUuid_strategy_nullCode_throws() {
            PromoPayoutContext ctx = baseContext(); // vendorCampaignCode null

            assertThatThrownBy(() -> runEnrich(ctx,
                    new PromoPayoutConfig().campaignResolveStrategy(CampaignResolveStrategy.PLAYER_UUID)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("vendorCampaignCode");
        }

        @Test
        @DisplayName("VENDOR_LINE_AND_CODE strategy with null vendorCampaignCode -> IllegalStateException")
        void vendorLineAndCode_strategy_nullCode_throws() {
            PromoPayoutContext ctx = baseContext(); // vendorCampaignCode null

            assertThatThrownBy(() -> runEnrich(ctx,
                    new PromoPayoutConfig().campaignResolveStrategy(CampaignResolveStrategy.VENDOR_LINE_AND_CODE)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("vendorCampaignCode");
        }

        private void runEnrich(PromoPayoutContext ctx, PromoPayoutConfig config) {
            LogContext lc = mock(LogContext.class);
            when(lc.getStart()).thenReturn(REQUEST_RECEIVE_TIME);
            when(lc.getTraceId()).thenReturn("trace-test-001");
            try (MockedStatic<LogContextHolder> holder = mockStatic(LogContextHolder.class);
                 MockedStatic<PromoPayoutContextHolder> cfgHolder = mockStatic(PromoPayoutContextHolder.class)) {
                holder.when(LogContextHolder::get).thenReturn(lc);
                cfgHolder.when(PromoPayoutContextHolder::getConfig).thenReturn(config);
                enricher.doEnrich(ctx);
            }
        }
    }
}
