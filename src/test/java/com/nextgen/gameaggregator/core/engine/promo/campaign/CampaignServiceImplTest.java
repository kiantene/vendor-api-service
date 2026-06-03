package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.promo.api.PromoEngineApiAdapter;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @InjectMocks
    private CampaignServiceImpl campaignService;

    @Mock private PromoEngineApiAdapter apiAdapter;
    @Mock private FetchCampaignRequestMapper requestMapper;
    @Mock private FetchCampaignResponseMapper responseMapper;

    private String traceId;

    @BeforeEach
    void setUp() {
        LogContext logContext = new LogContext();
        traceId = logContext.getTraceId();
        LogContextHolder.set(logContext);
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @Test
    void getCampaignByPlayerUuid_buildsRequestWithPlayerUuidAndReturnsCampaign() {
        String playerUuid = "player-uuid-123";
        ApiRequest apiRequest = mock(ApiRequest.class);
        ApiResult apiResult = mock(ApiResult.class);
        FetchCampaignResponse response = new FetchCampaignResponse();
        Campaign expected = Campaign.builder().uuid("campaign-uuid").campaignName("Free Spins").build();

        when(apiAdapter.ofFetchCampaignByPlayer(any(),
                argThat(req -> playerUuid.equals(req.getPlayerUuid()) && traceId.equals(req.getTraceId())))).thenReturn(apiRequest);
        when(apiAdapter.execute(eq(apiRequest), any())).thenReturn(apiResult);
        when(apiResult.parseTo(FetchCampaignResponse.class)).thenReturn(response);
        when(responseMapper.responseToCampaign(response)).thenReturn(expected);

        Campaign result = campaignService.getCampaignByPlayerUuid(playerUuid);

        assertEquals(expected, result);
        verify(apiAdapter).ofFetchCampaignByPlayer(any(),
                argThat(req -> playerUuid.equals(req.getPlayerUuid()) && traceId.equals(req.getTraceId())));
        verify(apiAdapter, never()).ofFetchCampaign(any(), any());
    }
}
