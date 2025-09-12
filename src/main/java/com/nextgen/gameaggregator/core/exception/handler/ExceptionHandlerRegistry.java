package com.nextgen.gameaggregator.core.exception.handler;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry for exception handlers
 */
public final class ExceptionHandlerRegistry {

    private ExceptionHandlerRegistry() {
        // Utility class
    }

    /**
     * Create and return the exception handler mappings
     */
    public static Map<Class<? extends Exception>, Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>>> createHandlers() {
        Map<Class<? extends Exception>, Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>>> handlers = new HashMap<>();

        // Business Logic Exceptions
        registerBusinessExceptions(handlers);

        // Special transformation exceptions
        registerTransformationExceptions(handlers);

        // System exceptions
        registerSystemExceptions(handlers);

        return Map.copyOf(handlers); // Immutable copy
    }

    private static void registerBusinessExceptions(Map<Class<? extends Exception>, Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>>> handlers) {
        handlers.put(GameSessionExpiredException.class,
                mapper -> ex -> mapper.onGameSessionExpired((GameSessionExpiredException) ex));

        handlers.put(GameTerminatedException.class,
                mapper -> ex -> mapper.onGameTerminated((GameTerminatedException) ex));

        handlers.put(InsufficientBalanceException.class,
                mapper -> ex -> mapper.onInsufficientBalance((InsufficientBalanceException) ex));

        handlers.put(PlayerDisabledException.class,
                mapper -> ex -> mapper.onPlayerDisabled((PlayerDisabledException) ex));

        handlers.put(BetNotAllowedException.class,
                mapper -> ex -> mapper.onBetNotAllowed((BetNotAllowedException) ex));

        handlers.put(DuplicateBetException.class,
                mapper -> ex -> mapper.onDuplicateBet((DuplicateBetException) ex));

        handlers.put(DuplicateRequestException.class,
                mapper -> ex -> mapper.onDuplicateRequest((DuplicateRequestException) ex));

        handlers.put(BetNotFoundException.class,
                mapper -> ex -> mapper.onBetNotFound((BetNotFoundException) ex));

        handlers.put(RollbackNotAllowedException.class,
                mapper -> ex -> mapper.onRollbackNotAllowed((RollbackNotAllowedException) ex));
    }

    private static void registerTransformationExceptions(Map<Class<? extends Exception>, Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>>> handlers) {
        // Exceptions that need transformation before handling
        handlers.put(EntityNotFoundException.class,
                mapper -> ex -> mapper.onInternalConfigurationError(new InternalConfigurationException(ex.getMessage(), ex)));

        handlers.put(InvalidOperatorResponseException.class,
                mapper -> ex -> mapper.onInternalError(new InternalServerException(ex.getMessage(), ex)));
    }

    private static void registerSystemExceptions(Map<Class<? extends Exception>, Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>>> handlers) {
        handlers.put(InternalConfigurationException.class,
                mapper -> ex -> mapper.onInternalConfigurationError((InternalConfigurationException) ex));
    }
}
