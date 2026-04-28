package com.nextgen.gameaggregator.core.engine.promo.player;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.common.LoggingApiAdapterLifecycle;
import com.nextgen.gameaggregator.core.engine.promo.api.PromoEngineApiAdapter;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.data.VendorCampaignGameDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveCampaignService {

    private final PromoEngineApiAdapter apiAdapter;
    private final VendorCampaignGameDataService vendorCampaignGameDataService;

    public String getVendorCampaignCode(GameSession gameSession) {
        try {
            if (vendorCampaignGameDataService.getIsSupportFreeRound(gameSession.getVendorGameCode()) == 0) {
                return null;
            }

            LogContext logContext = LogContextHolder.get().copy();
            String traceId = logContext.getTraceId();

            FindActivePlayerCampaignRequest request = FindActivePlayerCampaignRequest.builder()
                    .traceId(traceId)
                    .currencyCode(gameSession.getCurrencyCode())
                    .vendorLineId(gameSession.getVendorLineId())
                    .vendorPlayerUsername(gameSession.getVendorPlayerUsername())
                    .vendorGameCode(gameSession.getVendorGameCode())
                    .build();

            ApiRequest apiRequest = apiAdapter.ofFetchPlayerActiveCampaign(traceId, request);
            ApiResult apiResult = apiAdapter.execute(apiRequest, new LoggingApiAdapterLifecycle(logContext));
            apiResult.throwIfError();

            FindActivePlayerCampaignResponse response = apiResult.parseTo(FindActivePlayerCampaignResponse.class);
            FindActivePlayerCampaignResponse.Data data = response.getData();

            return data != null ? data.getVendorCampaignCode() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch active player campaign, game launch will proceed without eventId: {}", e.getMessage());
            return null;
        }
    }
}
