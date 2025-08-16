package com.nextgen.gameaggregator.aspect;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapperRegistry;
import com.nextgen.gameaggregator.core.exception.handler.VendorExceptionHandlerService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class VendorExceptionAspect {
    private final VendorExceptionMapperRegistry registry;
    private final VendorExceptionHandlerService exceptionHandlerService;

    public VendorExceptionAspect(VendorExceptionMapperRegistry registry,
                                 VendorExceptionHandlerService exceptionHandlerService) {
        this.registry = registry;
        this.exceptionHandlerService = exceptionHandlerService;
    }

    @Around("@annotation(vendorHandler)")
    public Object handleVendorExceptions(ProceedingJoinPoint joinPoint,
                                         VendorExceptionHandler vendorHandler) throws Throwable {

        String vendorClassName = vendorHandler.className();
        VendorExceptionMapper mapper = registry.getMapper(vendorClassName);

        if (mapper == null) {
            throw new InternalConfigurationException("No exception mapper registered for vendor: " + vendorClassName);
        }

        LogContext logContext = LogContextHolder.get();

        try {
            return joinPoint.proceed();

        } catch (Exception ex) {
            return handleException(ex, mapper, logContext);
        }
    }

    private ResponseEntity<?> handleException(Exception ex, VendorExceptionMapper mapper, LogContext logContext) {
        // Log the exception
        logContext.setException(ex);

        // Handle special logging case for InvalidOperatorResponseException
        if (ex instanceof InvalidOperatorResponseException opEx) {
            logContext.setRootCause(opEx.getRootCause());
        }

        // Delegate actual exception handling to the dedicated handler
        VendorErrorResponse errorResponse = exceptionHandlerService.handleException(ex, mapper);

        // Final fallback if handler returns null
        if (errorResponse == null) {
            String exceptionInfo = logContext.getException();
            logContext.setRootCause(exceptionInfo + " is not handled");
            return exceptionHandlerService.createDefaultErrorResponse(exceptionInfo);
        }

        return new ResponseEntity<>(errorResponse.getBody(), errorResponse.getStatusCode());
    }
}
