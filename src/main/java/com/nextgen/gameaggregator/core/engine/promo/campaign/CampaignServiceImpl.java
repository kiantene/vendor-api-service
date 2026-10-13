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

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CampaignServiceImpl implements CampaignService {
    private final PromoEngineApiAdapter apiAdapter;
    private final FetchCampaignRequestMapper fetchCampaignRequestMapper;
    private final FetchCampaignResponseMapper fetchCampaignResponseMapper;
    private final CreatePayoutCampaignRequestMapper createPayoutCampaignRequestMapper;
    private final CreatePayoutCampaignResponseMapper createPayoutCampaignResponseMapper;

    @Override
    public Campaign getCampaign(String vendorCampaignCode, Integer vendorLineId, Integer promoType) {
        LogContext logContext = LogContextHolder.get().copy();
        String traceId = logContext.getTraceId();

        ApiRequest apiRequest = apiAdapter.ofFetchCampaign(
                traceId,
                fetchCampaignRequestMapper.toFetchCampaignRequest(traceId, vendorCampaignCode, vendorLineId, promoType)
        );
        ApiResult apiResult = apiAdapter.execute(apiRequest, new LoggingApiAdapterLifecycle(logContext));
        apiResult.throwIfError();
        FetchCampaignResponse response = apiResult.parseTo(FetchCampaignResponse.class);

        return fetchCampaignResponseMapper.responseToCampaign(response);
    }

    @Override
    public Campaign getCampaignByPlayerUuid(String playerUuid) {
        LogContext logContext = LogContextHolder.get().copy();
        String traceId = logContext.getTraceId();

        ApiRequest apiRequest = apiAdapter.ofFetchCampaignByPlayer(
                traceId,
                FetchCampaignByPlayerRequest.builder().traceId(traceId).playerUuid(playerUuid).build()
        );
        ApiResult apiResult = apiAdapter.execute(apiRequest, new LoggingApiAdapterLifecycle(logContext));
        apiResult.throwIfError();
        FetchCampaignResponse response = apiResult.parseTo(FetchCampaignResponse.class);

        return fetchCampaignResponseMapper.responseToCampaign(response);
    }

    @Override
    public Campaign getCampaignByRef(CampaignResolveStrategy strategy, Map<String, String> params) {
        LogContext logContext = LogContextHolder.get().copy();
        String traceId = logContext.getTraceId();

        ApiRequest apiRequest = apiAdapter.ofResolveCampaign(
                traceId,
                ResolveCampaignRequest.builder().traceId(traceId).strategy(strategy).params(params).build()
        );
        ApiResult apiResult = apiAdapter.execute(apiRequest, new LoggingApiAdapterLifecycle(logContext));
        apiResult.throwIfError();
        FetchCampaignResponse response = apiResult.parseTo(FetchCampaignResponse.class);

        return fetchCampaignResponseMapper.responseToCampaign(response);
    }

    @Override
    public Campaign createPayoutCampaign(Integer promoType, Integer vendorId, Integer vendorLineId, String vendorCampaignCode, String vendorCampaignName, String vendorTransactionId, String currencyCode, String vendorCurrencyCode, Long payoutTime) {
        LogContext logContext = LogContextHolder.get().copy();
        String traceId = logContext.getTraceId();

        ApiRequest apiRequest = apiAdapter.ofCreatePayoutCampaign(
                traceId,
                createPayoutCampaignRequestMapper.toCreatePayoutCampaignRequest(traceId, promoType, vendorId, vendorLineId, vendorCampaignCode, vendorCampaignName, vendorTransactionId, currencyCode, vendorCurrencyCode, payoutTime)
        );
        ApiResult apiResult = apiAdapter.execute(apiRequest, new LoggingApiAdapterLifecycle(logContext));
        apiResult.throwIfError();

        CreatePayoutCampaignResponse response = apiResult.parseTo(CreatePayoutCampaignResponse.class);
        return createPayoutCampaignResponseMapper.responseToCampaign(response);
    }
}
