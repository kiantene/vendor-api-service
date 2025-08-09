package com.nextgen.gameaggregator.core.engine.wallet;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.DuplicateBetException;
import com.nextgen.gameaggregator.core.exception.InternalServerException;
import com.nextgen.gameaggregator.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WalletExceptionTranslator {

    /**
     * Translates deprecated exceptions to current domain exceptions.
     * TODO: Remove this class once deprecated exceptions are fully replaced in upstream services
     */
    public void translateAndThrow(Exception ex) {
        if (ex instanceof InvalidAgentApiCredentialException || ex instanceof VendorCurrencyNotSupportException) {
            throw new InternalConfigurationException(ex.getMessage(), ex);
        }

        if (ex instanceof BetResultIdempotentViolationException idempotentEx) {
            throw new DuplicateBetException(idempotentEx.getBetId());
        }

        if (ex instanceof TransactionStillProcessingException) {
            throw new DuplicateBetException(ex.getMessage());
        }

        if (ex instanceof InsufficientBalanceException) {
            throw new com.nextgen.gameaggregator.core.exception.InsufficientBalanceException();
        }

        // Handle unexpected exceptions
        log.error("Unexpected exception during wallet operation", ex);
        throw new InternalServerException("Unexpected error during wallet operation", ex);
    }
}
