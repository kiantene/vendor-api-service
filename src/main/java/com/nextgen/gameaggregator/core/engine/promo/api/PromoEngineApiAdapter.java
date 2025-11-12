package com.nextgen.gameaggregator.core.engine.promo.api;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.core.api.BlockingApiAdapter;
import com.nextgen.gameaggregator.core.engine.promo.campaign.FetchCampaignRequest;
import com.nextgen.gameaggregator.core.util.OperatorSignatureUtil;
import com.nextgen.gameaggregator.promoengine.PromoEngineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PromoEngineApiAdapter extends BlockingApiAdapter<ApiRequest, ApiResult> {
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String FETCH_CAMPAIGN = "/internal/fetchCampaign";

    private final PromoEngineProperties promoEngineProperties;

    @Override
    protected ApiResult onApiSuccess(ApiResult apiResult) {
        return apiResult;
    }

    @Override
    protected ApiResult onApiError(ApiResult apiResult) {
        return apiResult;
    }

    @Override
    protected ApiResult onApiComplete(ApiResult apiResult) {
        return apiResult;
    }

    public ApiRequest ofFetchCampaign(String traceId, FetchCampaignRequest request) {
        String signature = OperatorSignatureUtil.sign(request, promoEngineProperties.getApiSecret());

        return ApiRequest.builder()
                .traceId(traceId)
                .method(HttpMethod.POST)
                .baseUrl(promoEngineProperties.getHost())
                .path(FETCH_CAMPAIGN)
                .headers(Map.of(HEADER_SIGNATURE, signature))
                .body(request)
                .build();
    }
}
