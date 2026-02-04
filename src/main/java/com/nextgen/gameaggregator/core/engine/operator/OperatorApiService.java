package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.context.OperatorRequestContext;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperatorApiService {

    private final ClientRequestService clientRequestService;
    private final OperatorApiAdapter operatorApiAdapter;

    /**
     * Executes an operator API request, validates the response, and handles exceptions.
     *
     * <p>
     * Important notes:
     * - VendorRequestContext is currently passed in only to enrich exceptions.
     *   TODO: Consider removing it by attaching vendor metadata to OperatorApiRequest or wrapping in a single execution context.
     * - This method returns PlayerBalanceData, which ties this service to wallet/balance endpoints. Consider making it generic for other API responses.
     * - Exceptions are caught broadly (Exception) — this may mask programming errors (NPE, ClassCastException). Consider catching only expected API or network exceptions.
     * </p>
     *
     * @param adapter      different wallet adapter (i.e: BetAdapter, ResultAdapter, etc)
     * @param context      operator request context, includes request payload, scenario, and round
     * @param vendorContext vendor metadata currently used for exception enrichment
     * @return the PlayerBalanceData returned by the operator API, or fallback if exception occurs
     */
    public PlayerBalanceData execute(OperatorRequestAdapter adapter, OperatorRequestContext<OperatorRequestObject, OperatorScenario> context, VendorRequestContext vendorContext) {

        OperatorApiRequest apiRequest = clientRequestService.createOperatorApiRequest(context);

        try {

            ApiResult apiResult = operatorApiAdapter.execute(apiRequest);
            apiResult.throwIfError();

            ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

            adapter.validateClientResponse(response, context, vendorContext);

            return response.getData();

        } catch (Exception ex) {
            log.warn("Exception occurred during operator API execution. TraceId={}, Error={}",
                    context.request().getTraceId(),
                    ex.getMessage(), ex);
            // Delegate exception handling to the adapter
            // The adapter may implement retries, fallback balance, or domain-specific exception mapping
            return adapter.handleException(ex, apiRequest, context);
        }
    }
}
