package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.common.LoggingApiAdapterLifecycle;
import com.nextgen.gameaggregator.core.engine.promo.api.PromoEngineApiAdapter;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CampaignServiceImpl implements CampaignService {
    private final PromoEngineApiAdapter apiAdapter;
    private final FetchCampaignRequestMapper requestMapper;
    private final FetchCampaignResponseMapper responseMapper;

    @Override
    public Campaign getCampaign(String vendorCampaignCode, Integer vendorLineId, Integer promoType) {
        LogContext logContext = LogContextHolder.get().copy();
        String traceId = logContext.getTraceId();

        ApiRequest apiRequest = apiAdapter.ofFetchCampaign(
                traceId,
                requestMapper.toFetchCampaignRequest(traceId, vendorCampaignCode, vendorLineId, promoType)
        );
        ApiResult apiResult = apiAdapter.execute(apiRequest, new LoggingApiAdapterLifecycle(logContext));
        apiResult.throwIfError();
        FetchCampaignResponse response = apiResult.parseTo(FetchCampaignResponse.class);

        return responseMapper.responseToCampaign(response);
    }
}
