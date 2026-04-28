package com.nextgen.gameaggregator.core.engine.promo.api;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.core.api.BlockingApiAdapter;
import com.nextgen.gameaggregator.core.engine.promo.campaign.FetchCampaignRequest;
import com.nextgen.gameaggregator.core.engine.promo.player.FindActivePlayerCampaignRequest;
import com.nextgen.gameaggregator.core.util.OperatorSignatureUtil;
import com.nextgen.gameaggregator.promoengine.PromoEngineProperties;
import com.nextgen.gameaggregator.promoengine.constant.EndPoints;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PromoEngineApiAdapter extends BlockingApiAdapter<ApiRequest, ApiResult> {
    public static final String HEADER_SIGNATURE = "X-Signature";

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
        return buildRequest(traceId, EndPoints.FETCH_CAMPAIGN, request);
    }

    public ApiRequest ofFetchPlayerActiveCampaign(String traceId, FindActivePlayerCampaignRequest request) {
        return buildRequest(traceId, EndPoints.FETCH_PLAYER_ACTIVE_CAMPAIGN, request);
    }

    private ApiRequest buildRequest(String traceId, String path, Object body) {
        return ApiRequest.builder()
                .traceId(traceId)
                .method(HttpMethod.POST)
                .baseUrl(promoEngineProperties.getHost())
                .path(path)
                .headers(Map.of(HEADER_SIGNATURE, OperatorSignatureUtil.sign(body, promoEngineProperties.getApiSecret())))
                .body(body)
                .build();
    }
}
