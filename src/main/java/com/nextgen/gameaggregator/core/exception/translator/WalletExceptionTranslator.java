package com.nextgen.gameaggregator.core.exception.translator;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.DuplicateBetException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Translates deprecated exceptions to current domain exceptions.
 * TODO: Remove this class once deprecated exceptions are fully replaced in upstream services
 */
@Component
@Slf4j
public class WalletExceptionTranslator {

    /** WalletBet exceptions
     * InsufficientBalanceException - re-mapped
     * CouchbaseDataIntegrityException - throw as InternalServerException
     * InvalidOperatorResponseException - throw as InternalServerException
     * InvalidAgentApiCredentialException - throw as InternalConfigurationException
     * BetResultIdempotentViolationException - throw as DuplicateBetException
     * TransactionStillProcessingException - throw as DuplicateBetException
     * VendorCurrencyNotSupportException - throw as InternalConfigurationException
     */

    /** WalletBetResult exceptions
     * BetNotFoundException – throw as InternalServerException
     * InvalidOperatorResponseException - throw as InternalServerException
     * InvalidAgentApiCredentialException - throw as InternalConfigurationException
     * MergedBetDataIntegrityException - throw as InternalServerException
     * InsufficientBalanceException - re-mapped
     * TransactionStillProcessingException - throw as DuplicateBetException
     * BetResultIdempotentViolationException - throw as DuplicateBetException
     * VendorCurrencyNotSupportException - throw as InternalConfigurationException
     * InternalServerTimeoutRetryException - throw as InternalServerException
     */

    /** WalletRollback exceptions
     * BetNotFoundException – throw as InternalServerException
     * RecordNotFoundException – Generic exception for orphan records
     * InvalidAgentApiCredentialException - throw as InternalConfigurationException
     * InvalidOperatorResponseException - throw as InternalServerException
     * BetRefundIdempotentViolationException - throw as DuplicateRequestException
     * BetResultIdempotentViolationException - throw as DuplicateBetException
     * TransactionStillProcessingException - throw as DuplicateBetException
     * VendorCurrencyNotSupportException - throw as InternalConfigurationException
     * InvalidFormatException - throw as InternalServerException
     */

    /**
     * For BetNotFoundException in WalletBetResult, this is typically thrown when a result is received but cannot find a corresponding bet.
     * TODO: add new logic to handle in new WalletxxxWrapper class
     * Two scenarios:
     * 1. Result is received first, then followed by Bet transaction.
     * 2. Bet is settled with a result, but vendor resends the same result.
     *
     * TODO: For BetNotFoundException in WalletRollback
     *
     *
     */
    private static final Set<Class<? extends Exception>> INTERNAL_CONFIG_EXCEPTIONS = Set.of(
            InvalidAgentApiCredentialException.class,
            VendorCurrencyNotSupportException.class
    );

    private static final Set<Class<? extends Exception>> BET_NOT_ALLOWED_EXCEPTIONS = Set.of(
            DisabledVendorLineException.class,
            DisabledGameException.class
    );

    public RuntimeException translate(Exception ex) {
        RuntimeException translatedException = null;

        /**
         * The current AuthenticationException is deprecated.
         * To catch and throw as GameSessionExpiredException.
         */
        if (ex instanceof AuthenticationException) {
            translatedException = new GameSessionExpiredException(ex.getMessage());
        }

        /**
         * The current GameTerminatedException is deprecated.
         * To catch and re-throw from the core library GameTerminatedException.
         */
        if (ex instanceof GameTerminatedException) {
            translatedException = new com.nextgen.gameaggregator.core.exception.GameTerminatedException(ex.getMessage());
        }

        /**
         * The current InsufficientBalanceException is deprecated.
         * To catch and re-throw from the core library InsufficientBalanceException.
         */
        if (ex instanceof InsufficientBalanceException) {
            translatedException = new com.nextgen.gameaggregator.core.exception.InsufficientBalanceException();
        }

        if (ex instanceof DisabledAgentPlayerException) {
            translatedException = new PlayerDisabledException(ex.getMessage());
        }

        /**
         * DisabledVendorLineException.class
         * When vendor line is disabled, means we want to stop all transactions coming from this line.
         * As such, all new bets should not be processed.
         * However, if there are bet results still coming through, we should still accept so that existing bets
         * can settle as per normal and not throw any exception.
         *
         * Same behavior for DisabledGameException
         */
        if (isBetNotAllowedException(ex)) {
            translatedException = new com.nextgen.gameaggregator.core.exception.BetNotAllowedException(ex.getMessage(), ex);
        }

        /**
         * InvalidPlayerException will be thrown if vendorPlayerUsername cannot be found.
         * This could be related to wrong username sent by the vendor, or record is missing in database.
         */
        if (ex instanceof InvalidPlayerException) {
            translatedException = new com.nextgen.core.exception.InvalidRequestException("Player cannot be found");
        }

        if (ex instanceof BetResultIdempotentViolationException idempotentEx) {
            translatedException = new DuplicateBetException(idempotentEx.getBetId());
        }

        /**
         * TransactionStillProcessingException is thrown when operator has not completed processing
         * but vendor resent the request.
         * This is usually due to vendor's timeout faster than operator's.
         * For bet transactions, most vendors should send a cancel bet instead of retry, therefore these vendors will
         * not encounter this exception.
         * If vendor indeed sends a duplicate bet request, we should block it by default and not send to operator.
         *
         * These are the following scenarios:
         * 1. Operator fails, GA returns error to vendor
         * 2. Operator process longer, GA did not respond to vendor, but vendor retry - block duplicate request
         * 3. Operator success, GA return success to vendor, but vendor retry - block duplicate request
         *
         * TODO: However, for #2:
         * If Operator process longer, but vendor sends a cancel bet,
         * GA must always accept the cancel bet request and only sends to Operator
         * after receiving a response (regardless success or fail) from Operator.
         */
        if (ex instanceof TransactionStillProcessingException) {
            translatedException = new DuplicateBetException(ex.getMessage());
        }

        /**
         * InvalidAgentApiCredentialException
         * VendorCurrencyNotSupportException
         */
        if (isInternalConfigurationException(ex)) {
            translatedException = new InternalConfigurationException(ex.getMessage(), ex);
        }

        if (ex instanceof BetNotFoundException) {
            throw new com.nextgen.gameaggregator.core.exception.BetNotFoundException(ex.getMessage(), ex);
        }

        if (translatedException != null) return translatedException;

        if (ex instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        return new RuntimeException(ex.getMessage(), ex);
    }

    private boolean isInternalConfigurationException(Exception ex) {
        return INTERNAL_CONFIG_EXCEPTIONS.stream().anyMatch(clazz -> clazz.isInstance(ex));
    }

    private boolean isBetNotAllowedException(Exception ex) {
        return BET_NOT_ALLOWED_EXCEPTIONS.stream().anyMatch(clazz -> clazz.isInstance(ex));
    }
}
