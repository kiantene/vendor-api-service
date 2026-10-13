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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock private CreatePayoutCampaignRequestMapper createPayoutCampaignRequestMapper;
    @Mock private CreatePayoutCampaignResponseMapper createPayoutCampaignResponseMapper;

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

    @Test
    void getCampaignByRef_buildsResolveRequestWithStrategyAndParamsAndReturnsCampaign() {
        Map<String, String> params = Map.of("username", "user-1", "freeRoundBonusId", "frb-1");
        ApiRequest apiRequest = mock(ApiRequest.class);
        ApiResult apiResult = mock(ApiResult.class);
        FetchCampaignResponse response = new FetchCampaignResponse();
        Campaign expected = Campaign.builder().uuid("campaign-uuid").campaignName("Free Rounds").build();

        when(apiAdapter.ofResolveCampaign(any(),
                argThat(req -> req.getStrategy() == CampaignResolveStrategy.USERNAME_AND_BONUS_ID
                        && params.equals(req.getParams())
                        && traceId.equals(req.getTraceId())))).thenReturn(apiRequest);
        when(apiAdapter.execute(eq(apiRequest), any())).thenReturn(apiResult);
        when(apiResult.parseTo(FetchCampaignResponse.class)).thenReturn(response);
        when(responseMapper.responseToCampaign(response)).thenReturn(expected);

        Campaign result = campaignService.getCampaignByRef(CampaignResolveStrategy.USERNAME_AND_BONUS_ID, params);

        assertEquals(expected, result);
        verify(apiAdapter).ofResolveCampaign(any(),
                argThat(req -> req.getStrategy() == CampaignResolveStrategy.USERNAME_AND_BONUS_ID
                        && params.equals(req.getParams())));
        verify(apiAdapter, never()).ofFetchCampaignByPlayer(any(), any());
    }

    @Test
    void createPayoutCampaign_postsTheMappedRequestAndReturnsTheCreatedCampaign() {
        CreatePayoutCampaignRequest request = CreatePayoutCampaignRequest.builder().traceId(traceId).build();
        ApiRequest apiRequest = mock(ApiRequest.class);
        ApiResult apiResult = mock(ApiResult.class);
        CreatePayoutCampaignResponse response = new CreatePayoutCampaignResponse();
        Campaign expected = Campaign.builder().uuid("campaign-uuid").campaignName("TOURNAMENT 12345").build();

        when(createPayoutCampaignRequestMapper.toCreatePayoutCampaignRequest(
                traceId, 2, 10, 100, "12345", "SUMMER_SPRINT", "tx-1", "USD", "USD", 1_700_000_000_000L))
                .thenReturn(request);
        when(apiAdapter.ofCreatePayoutCampaign(traceId, request)).thenReturn(apiRequest);
        when(apiAdapter.execute(eq(apiRequest), any())).thenReturn(apiResult);
        when(apiResult.parseTo(CreatePayoutCampaignResponse.class)).thenReturn(response);
        when(createPayoutCampaignResponseMapper.responseToCampaign(response)).thenReturn(expected);

        Campaign result = campaignService.createPayoutCampaign(
                2, 10, 100, "12345", "SUMMER_SPRINT", "tx-1", "USD", "USD", 1_700_000_000_000L);

        assertEquals(expected, result);
        verify(apiResult).throwIfError();
        verify(apiAdapter, never()).ofFetchCampaign(any(), any());
    }

    @Test
    void createPayoutCampaign_propagatesAnHttpError() {
        ApiRequest apiRequest = mock(ApiRequest.class);
        ApiResult apiResult = mock(ApiResult.class);

        when(apiAdapter.ofCreatePayoutCampaign(any(), any())).thenReturn(apiRequest);
        when(apiAdapter.execute(eq(apiRequest), any())).thenReturn(apiResult);
        doThrow(new IllegalStateException("HTTP 400")).when(apiResult).throwIfError();

        assertThrows(IllegalStateException.class, () -> campaignService.createPayoutCampaign(
                6, 10, 100, null, null, "tx-2", "EUR", "EUR", null));
        verify(apiResult, never()).parseTo(any());
        verifyNoInteractions(createPayoutCampaignResponseMapper);
    }
}
