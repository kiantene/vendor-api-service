package com.nextgen.gameaggregator.core.engine.operator.wallet.debit;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallAdapter;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter for the debit wallet-call. Used by {@code OperatorApiService.execute(...)} to
 * validate the operator's response.
 *
 * <p>Default {@code handleException} from {@link OperatorCallAdapter} re-throws
 * {@code InsufficientBalanceException} and wraps anything else in {@code OperatorApiException} —
 * good enough for the debit path (insufficient balance is the one case that needs distinct
 * downstream handling).
 */
@Component
@RequiredArgsConstructor
public class DebitOperatorWalletAdapter implements OperatorCallAdapter {

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
                "wallet.debit",
                requestDto.getTraceId(),
                requestDto.getUsername(),
                requestDto.getCurrency(),
                true
        );
    }
}
