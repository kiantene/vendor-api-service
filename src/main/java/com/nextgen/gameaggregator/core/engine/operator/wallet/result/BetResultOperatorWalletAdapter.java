package com.nextgen.gameaggregator.core.engine.operator.wallet.result;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.context.OperatorRequestContext;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.operator.OperatorRequestAdapter;
import com.nextgen.gameaggregator.core.engine.operator.OperatorScenario;
import com.nextgen.gameaggregator.core.retry.RetryHelper;
import com.nextgen.gameaggregator.core.retry.RetryPolicy;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BetResultOperatorWalletAdapter implements OperatorRequestAdapter {

    protected ClientResponseValidator clientResponseValidator;
    protected RetryQueueService retryQueueService;

    @Override
    public void validateClientResponse(ClientApiResponse response, OperatorRequestContext<OperatorRequestObject, OperatorScenario> context, VendorRequestContext vendorContext) throws InvalidOperatorResponseException {
        clientResponseValidator.validate(
                response,
                buildValidationRecord(context.request()),
                vendorContext
            );
    }

    private ClientResponseValidator.RequestRecord buildValidationRecord(OperatorRequestObject request) {
        return new ClientResponseValidator.RequestRecord(
                "wallet.result",
                request.getTraceId(),
                request.getUsername(),
                request.getCurrency(),
                true
        );
    }

    @Override
    public PlayerBalanceData handleException(Exception ex, OperatorApiRequest apiRequest, OperatorRequestContext<OperatorRequestObject, OperatorScenario> context) {

        if (context.scenario().shouldRetry()) {
            if (RetryPolicy.shouldRetry(getRetryOrigin(), ex)) {
                retryQueueService.enqueue(
                        RetryHelper.toHttpCallSpec(apiRequest),
                        getRetryOrigin()
                ).subscribe();
            }

            return getFallbackBalance(context.round());
        }

        return OperatorRequestAdapter.super.handleException(ex, apiRequest, context);
    }

    private RetryOrigin getRetryOrigin() {
        return RetryOrigin.BET_RESULT;
    }

    private PlayerBalanceData getFallbackBalance(GameRound round) {
        return PlayerBalanceData.getDefaultWithBalance(
                round.getAgentMeta().getUsername(),
                round.getAgentMeta().getCurrency(),
                round.getLastBalance()
        );
    }
}