package com.nextgen.gameaggregator.vendor.evolutionv2.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import lombok.Getter;

/**
 * Evolution v2 promo-payout integration.
 */
@Getter
public class EvolutionCallbackValidationException extends SignatureValidationException {
    private final ResponseCode responseCode;
    private final String uuid;

    public EvolutionCallbackValidationException(ResponseCode responseCode, String uuid) {
        super(responseCode.errorMessage);
        this.responseCode = responseCode;
        this.uuid = uuid;
    }
}
