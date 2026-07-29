package com.nextgen.gameaggregator.core.engine.operator.wallet.bet;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.context.OperatorRequestContext;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorRequestAdapter;
import com.nextgen.gameaggregator.core.engine.operator.OperatorScenario;
import com.nextgen.gameaggregator.core.validator.ClientResponseValidator;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BetOperatorWalletAdapter implements OperatorRequestAdapter {

    private final ClientResponseValidator clientResponseValidator;

    @Override
    public void validateClientResponse(ClientApiResponse response, OperatorRequestContext<OperatorRequestObject, OperatorScenario> context, VendorRequestContext vendorContext) throws InvalidOperatorResponseException {
        clientResponseValidator.validate(
                response,
                buildValidationRecord(context.request()),
                vendorContext
        );
    }

    private ClientResponseValidator.RequestRecord buildValidationRecord(OperatorRequestObject requestDto) {
        return new ClientResponseValidator.RequestRecord(
                "wallet.bet",
                requestDto.getTraceId(),
                requestDto.getUsername(),
                requestDto.getCurrency(),
                true
        );
    }
}
