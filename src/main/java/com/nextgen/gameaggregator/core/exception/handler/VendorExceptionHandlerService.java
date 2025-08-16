package com.nextgen.gameaggregator.core.exception.handler;

import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.InternalServerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class VendorExceptionHandlerService {

    private final Map<Class<? extends Exception>, Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>>> exceptionHandlers;

    public VendorExceptionHandlerService() {
        this.exceptionHandlers = ExceptionHandlerRegistry.createHandlers();
    }

    /**
     * Handle exception using the registry approach
     */
    public VendorErrorResponse handleException(Exception ex, VendorExceptionMapper mapper) {
        // Try to find specific handler
        VendorErrorResponse errorResponse = findAndExecuteHandler(ex, mapper);

        // Fallback to generic handler
        if (errorResponse == null) {
            errorResponse = handleGenericException(ex, mapper);
        }

        return errorResponse;
    }

    /**
     * Find and execute the appropriate exception handler
     */
    private VendorErrorResponse findAndExecuteHandler(Exception ex, VendorExceptionMapper mapper) {
        Class<? extends Exception> exceptionClass = ex.getClass();

        // Direct lookup
        Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>> handlerFactory = exceptionHandlers.get(exceptionClass);
        if (handlerFactory != null) {
            return handlerFactory.apply(mapper).apply(ex);
        }

        // Fallback: Check for inheritance hierarchy
        for (Map.Entry<Class<? extends Exception>, Function<VendorExceptionMapper, Function<Exception, VendorErrorResponse>>> entry : exceptionHandlers.entrySet()) {
            if (entry.getKey().isAssignableFrom(exceptionClass)) {
                return entry.getValue().apply(mapper).apply(ex);
            }
        }

        return null;
    }

    /**
     * Handle exceptions not in the registry
     */
    private VendorErrorResponse handleGenericException(Exception ex, VendorExceptionMapper mapper) {
        InternalServerException serverException = new InternalServerException(ex.getMessage(), ex);
        return mapper.onInternalError(serverException);
    }

    /**
     * Create default error response when all else fails
     */
    public ResponseEntity<?> createDefaultErrorResponse(String exceptionInfo) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Unhandled exception");
        String message = exceptionInfo != null && !exceptionInfo.isBlank()
                ? exceptionInfo
                : "Request failed";
        response.put("message", message);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
