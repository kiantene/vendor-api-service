package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignDataServiceTest {

    private CampaignCacheService cache;
    private CampaignService repository;
    private CampaignDataService dataService;

    @BeforeEach
    void setUp() {
        cache = mock(CampaignCacheService.class);
        repository = mock(CampaignService.class);
        dataService = new CampaignDataService(cache, repository);
    }

    @Test
    void getByPlayerUuid_returnsCampaignFromCache() {
        Campaign campaign = Campaign.builder().uuid("campaign-uuid").campaignName("Free Spins").build();
        when(cache.getByPlayerUuid("player-uuid-123")).thenReturn(campaign);

        Campaign result = dataService.getByPlayerUuid("player-uuid-123");

        assertEquals(campaign, result);
        verify(cache).getByPlayerUuid("player-uuid-123");
    }

    @Test
    void getByPlayerUuid_throwsEntityNotFoundException_whenCacheMiss() {
        when(cache.getByPlayerUuid("player-uuid-123")).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> dataService.getByPlayerUuid("player-uuid-123"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createPayoutCampaign_withVendorCode_goesThroughCacheAndCreatesOnMiss() {
        Campaign campaign = Campaign.builder().uuid("campaign-uuid").campaignName("TOURNAMENT 12345").build();
        when(repository.createPayoutCampaign(2, 10, 100, "12345", "SUMMER_SPRINT", "tx-1", "USD", "USD", 1_700_000_000_000L))
                .thenReturn(campaign);
        // a cache miss runs the supplier
        when(cache.createPayoutCampaign(eq(2), eq(100), eq("12345"), any()))
                .thenAnswer(invocation -> ((Supplier<Campaign>) invocation.getArgument(3)).get());

        Campaign result = dataService.createPayoutCampaign(2, 10, 100, "12345", "SUMMER_SPRINT", "tx-1", "USD", "USD", 1_700_000_000_000L);

        assertEquals(campaign, result);
        verify(cache).createPayoutCampaign(eq(2), eq(100), eq("12345"), any());
        verify(repository).createPayoutCampaign(2, 10, 100, "12345", "SUMMER_SPRINT", "tx-1", "USD", "USD", 1_700_000_000_000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createPayoutCampaign_withVendorCode_cacheHitSkipsPromoEngine() {
        Campaign cached = Campaign.builder().uuid("campaign-uuid").build();
        when(cache.createPayoutCampaign(eq(2), eq(100), eq("12345"), any())).thenReturn(cached);

        Campaign result = dataService.createPayoutCampaign(2, 10, 100, "12345", null, "tx-2", "USD", "USD", null);

        assertEquals(cached, result);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void createPayoutCampaign_withoutVendorCode_bypassesCache(String vendorCampaignCode) {
        // the promo engine derives one campaign per payout, so a cache entry would never be hit again
        Campaign campaign = Campaign.builder().uuid("campaign-uuid").build();
        when(repository.createPayoutCampaign(6, 10, 100, vendorCampaignCode, null, "tx-3", "USD", "USD", null))
                .thenReturn(campaign);

        Campaign result = dataService.createPayoutCampaign(6, 10, 100, vendorCampaignCode, null, "tx-3", "USD", "USD", null);

        assertEquals(campaign, result);
        verifyNoInteractions(cache);
    }
}
