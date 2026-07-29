package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignDataServiceTest {

    private CampaignCacheService cache;
    private CampaignDataService dataService;

    @BeforeEach
    void setUp() {
        cache = mock(CampaignCacheService.class);
        dataService = new CampaignDataService(cache);
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
}
