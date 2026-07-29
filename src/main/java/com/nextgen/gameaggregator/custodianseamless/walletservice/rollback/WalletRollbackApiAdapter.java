package com.nextgen.gameaggregator.custodianseamless.walletservice.rollback;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.core.api.BlockingApiAdapter;
import com.nextgen.gameaggregator.config.properties.WalletServiceProperties;
import com.nextgen.gameaggregator.core.util.OperatorSignatureUtil;
import com.nextgen.gameaggregator.custodianseamless.constant.WalletServiceEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WalletRollbackApiAdapter extends BlockingApiAdapter<ApiRequest, ApiResult> {
    private final WalletServiceProperties properties;

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

    public ApiRequest ofWalletTransactionRollback(String traceId, WalletRollbackRequest request, String apiKey, String apiSecret) {
        String signature = OperatorSignatureUtil.sign(request, apiSecret);

        return ApiRequest.builder()
                .traceId(traceId)
                .method(HttpMethod.POST)
                .baseUrl(properties.getHost())
                .path(WalletServiceEndpoints.WALLET_ROLLBACK)
                .headers(Map.of(WalletServiceEndpoints.HEADER_API_KEY, apiKey,
                        WalletServiceEndpoints.HEADER_SIGNATURE, signature))
                .body(request)
                .build();
    }

}
