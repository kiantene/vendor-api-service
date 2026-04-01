package com.nextgen.gameaggregator.aspect;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapperRegistry;
import com.nextgen.gameaggregator.core.exception.handler.VendorExceptionHandlerService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.registry.VendorResponseProcessorRegistry;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class VendorExceptionAspect {
    /**
     * This field holds an instance of {@link VendorExceptionMapperRegistry}.
     * It acts as a centralized registry of vendor-specific exception mappers.
     * Exception mappers retrieved from this registry are used to handle exceptions
     * that occur within the vendor-related processing flow.
     *
     * <p>
     * The registry maintains a mapping between vendor class names and their
     * corresponding exception mappers. This mapping ensures that the appropriate
     * exception mapper is used for a given vendor class, allowing for customized
     * exception handling based on the specific needs of each vendor.
     *</p>
     *
     * <p>
     * Responsibilities:
     * - Retrieves the appropriate {@link VendorExceptionMapper} instance for a
     *   given vendor class name using the {@link VendorExceptionMapperRegistry#getMapper(String)} method.
     * - Facilitates delegating exception handling to specific exception mappers
     *   to provide tailored responses for various exception scenarios.
     *</p>
     */
    private final VendorExceptionMapperRegistry registry;

    /**
     * A service responsible for handling exceptions that occur in vendor-related operations.
     * The exception handling logic involves mapping specific exception types to appropriate
     * error responses based on a registry of exception handlers.
     *
     * <p>
     * This service provides the following functionalities:
     * - Delegates the handling of exceptions to registered handlers based on the exception type.
     * - Falls back to a generic error handler if no specific handler is found.
     * - Generates consistent error responses for unhandled exceptions.
     * </p>
     *
     * Used by: {@link VendorExceptionAspect} to intercept methods annotated with {@link VendorExceptionHandler}.
     */
    private final VendorExceptionHandlerService exceptionHandlerService;

    /**
     * Registry for managing and retrieving {@link VendorResponsePostProcessor} instances
     * associated with specific vendor class names. The registry is used to determine
     * which response processor to invoke based on the vendor's class name.
     *
     * <p>
     * This registry is populated with available {@link VendorResponsePostProcessor}
     * implementations, allowing the application to dynamically resolve and execute
     * the appropriate response processor for a given vendor integration.
     * </p>
     */
    private final VendorResponseProcessorRegistry responseProcessorRegistry;

    public VendorExceptionAspect(VendorExceptionMapperRegistry registry,
                                 VendorExceptionHandlerService exceptionHandlerService,
                                 VendorResponseProcessorRegistry responseProcessorRegistry) {
        this.registry = registry;
        this.exceptionHandlerService = exceptionHandlerService;
        this.responseProcessorRegistry = responseProcessorRegistry;
    }

    /**
     * Intercepts method execution to handle vendor-specific exceptions using a registered exception mapper.
     * This method ensures that exceptions thrown during the execution of annotated methods are properly
     * converted into vendor-specific error responses.
     */
    @Around("@annotation(vendorHandler)")
    public Object handleVendorExceptions(ProceedingJoinPoint joinPoint,
                                         VendorExceptionHandler vendorHandler) throws Throwable {

        String vendorClassName = vendorHandler.className();
        VendorExceptionMapper mapper = registry.getMapper(vendorClassName);

        if (mapper == null) {
            throw new InternalConfigurationException("No exception mapper registered for vendor: " + vendorClassName);
        }

        try {
            return joinPoint.proceed();

        } catch (Exception exception) {
            Object[] args = joinPoint == null ? null : joinPoint.getArgs();

            return handleException(
                    exception,
                    mapper,
                    vendorClassName,
                    VendorExceptionContext.of(args, getRequestHeaders())
            );
        }
    }

    /**
     * Handles exceptions by delegating the error generation process to a vendor-specific exception handler
     * and optionally enriching the error response with additional processing.
     */
    private ResponseEntity<?> handleException(Exception ex, VendorExceptionMapper mapper, String vendorClassName, VendorExceptionContext errorContext) {
        LogContext logContext = LogContextHolder.get();

        logContext.setException(ex);

        // Handle a special logging case for InvalidOperatorResponseException
        if (ex instanceof InvalidOperatorResponseException opEx) {
            logContext.setRootCause(opEx.getRootCause());
        }

        // Delegate actual exception handling to the dedicated handler
        VendorErrorResponse errorResponse = exceptionHandlerService.handleException(ex, mapper);

        // Final fallback if handler returns null
        if (errorResponse == null) {
            String exceptionInfo = ex.getMessage();
            logContext.setRootCause(exceptionInfo + " is not handled");

            return exceptionHandlerService.createDefaultErrorResponse(exceptionInfo);
        }

        // Enrich Error if ResponseEnricher exists for Vendor
        VendorResponsePostProcessor postProcessor = responseProcessorRegistry.get(vendorClassName);
        if (postProcessor != null) {
            errorResponse = postProcessor.postProcessErrorResponse(errorResponse, errorContext);
        }

        if (errorResponse.hasHeaders()) {
            return new ResponseEntity<>(
                errorResponse.getBody(),
                errorResponse.getHeaders(),
                errorResponse.getStatusCode()
            );
        }

        return new ResponseEntity<>(errorResponse.getBody(), errorResponse.getStatusCode());
    }

    /**
     * Extracts the headers from the provided {@code ServletRequestAttributes} and returns them as a map.
     * Each entry in the map represents a request header name-value pair.
     * If the provided {@code ServletRequestAttributes} is null or no headers are present, this method returns null.
     */
    private static Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return headers;
        }

        HttpServletRequest request = attributes.getRequest();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        return headers;
    }
}
