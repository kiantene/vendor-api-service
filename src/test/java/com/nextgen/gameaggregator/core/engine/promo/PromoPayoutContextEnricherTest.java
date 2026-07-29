package com.nextgen.gameaggregator.core.engine.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.*;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.*;
import com.nextgen.gameaggregator.core.service.data.CampaignDataService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoPayoutContextEnricherTest {

    @InjectMocks
    private PromoPayoutContextEnricher enricher;

    @Mock private AgentPlayerDataService agentPlayerDataService;
    @Mock private VendorPlayerDataService vendorPlayerDataService;
    @Mock private VendorGameDataService vendorGameDataService;
    @Mock private CurrencyDataService currencyDataService;
    @Mock private VendorCurrencyDataService vendorCurrencyDataService;
    @Mock private VendorDataService vendorDataService;
    @Mock private AgentDataService agentDataService;
    @Mock private CampaignDataService campaignDataService;

    private PromoPayoutContext context;

    @BeforeEach
    void setUp() {
        context = PromoPayoutContext.builder()
                .vendorCampaignCode("campaign-player-uuid-123")
                .promoType(PromoType.FREE_ROUND)
                .build();
        context.getAgent().id(1);
        context.getVendor().id(2);
        context.getVendor().lineId(10);

        LogContextHolder.set(new LogContext());

        when(agentDataService.get(1)).thenReturn(new Agent());
        when(vendorDataService.get(2)).thenReturn(new Vendor());
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
        PromoPayoutContextHolder.clear();
    }

    @Test
    void populateCampaign_whenPlayerUuidLookupEnabled_callsGetByPlayerUuid() {
        PromoPayoutWrapperContext wrapper = new PromoPayoutWrapperContext(context);
        wrapper.getConfig().playerUuidCampaignLookup(true);
        PromoPayoutContextHolder.set(wrapper);

        Campaign campaign = Campaign.builder().uuid("campaign-uuid").campaignName("Test Campaign").build();
        when(campaignDataService.getByPlayerUuid("campaign-player-uuid-123")).thenReturn(campaign);

        enricher.doEnrich(context);

        verify(campaignDataService).getByPlayerUuid("campaign-player-uuid-123");
        verify(campaignDataService, never()).get(any(), any(), any());
        assertEquals("campaign-uuid", context.getCampaignUuid());
        assertEquals("Test Campaign", context.getVendorCampaignName());
    }

    @Test
    void populateCampaign_whenPlayerUuidLookupDisabled_callsGetByVendorCampaignCode() {
        PromoPayoutWrapperContext wrapper = new PromoPayoutWrapperContext(context);
        PromoPayoutContextHolder.set(wrapper);

        Campaign campaign = Campaign.builder().uuid("campaign-uuid").campaignName("Test Campaign").build();
        when(campaignDataService.get("campaign-player-uuid-123", 10, PromoType.FREE_ROUND.id)).thenReturn(campaign);

        enricher.doEnrich(context);

        verify(campaignDataService).get("campaign-player-uuid-123", 10, PromoType.FREE_ROUND.id);
        verify(campaignDataService, never()).getByPlayerUuid(any());
    }

    @Test
    void populateCampaign_whenVendorCampaignCodeIsNull_skipsLookup() {
        context = PromoPayoutContext.builder().build();
        context.getAgent().id(1);
        context.getVendor().id(2);

        PromoPayoutContextHolder.set(new PromoPayoutWrapperContext(context));

        enricher.doEnrich(context);

        verifyNoInteractions(campaignDataService);
    }
}
