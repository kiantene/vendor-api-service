package com.nextgen.gameaggregator.core.engine.operator.wallet.credit;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallAdapter;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter for the credit wallet-call. Used by {@code OperatorApiService.execute(...)}
 * to validate the operator's response.
 *
 * <p>The operator endpoint is the combined credit-or-rollback endpoint
 * ({@code EndPoints.WALLET_BET_CREDIT}); the inbound flow (settle vs refund) is encoded
 * in {@code OperatorCreditRequest.isRefund}.
 *
 * <p>A failed call is handled by the default {@code handleException}: it reaches the vendor, and is
 * never queued for async retry. A success code means the operator applied the credit.
 */
@Component
@RequiredArgsConstructor
public class CreditOperatorWalletAdapter implements OperatorCallAdapter {

    private final ClientResponseValidator clientResponseValidator;

    @Override
    public void validateClientResponse(ClientApiResponse response,
                                       OperatorCallContext<OperatorRequestObject> context,
                                       VendorRequestContext vendorContext) throws InvalidOperatorResponseException {
        clientResponseValidator.validate(
                response,
                buildValidationRecord(context.request()),
                vendorContext
        );
    }

    private ClientResponseValidator.RequestRecord buildValidationRecord(OperatorRequestObject requestDto) {
        return new ClientResponseValidator.RequestRecord(
                "wallet.credit",
                requestDto.getTraceId(),
                requestDto.getUsername(),
                requestDto.getCurrency(),
                true
        );
    }
}
