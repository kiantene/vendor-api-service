package com.nextgen.gameaggregator.core.exception.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestAttributes;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.InvalidRequestContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapperRegistry;
import com.nextgen.gameaggregator.core.registry.VendorResponseProcessorRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RequestValidationExceptionHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final VendorExceptionMapperRegistry mapperRegistry;
    private final RequestParserService requestParserService;
    private final VendorResponseProcessorRegistry responseProcessorRegistry;

    public RequestValidationExceptionHandler(VendorExceptionMapperRegistry registry,
                                             RequestParserService requestParserService,
                                             VendorResponseProcessorRegistry responseProcessorRegistry) {
        this.mapperRegistry = registry;
        this.requestParserService = requestParserService;
        this.responseProcessorRegistry = responseProcessorRegistry;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String vendorClassName = (String) request.getAttribute(RequestAttributes.VENDOR_CLASS_NAME);
        InvalidRequestException invalidRequestException = new InvalidRequestException(ex);
        if (vendorClassName == null) { // if no vendor class name not found, return default
            return handleDefaultValidation(invalidRequestException);
        }

        VendorExceptionMapper exceptionMapper = mapperRegistry.getMapper(vendorClassName);
        if (exceptionMapper == null) { // if no vendor handler set, return default
            return handleDefaultValidation(invalidRequestException);
        }

        VendorErrorResponse errorResponse = exceptionMapper.onInvalidRequestError(invalidRequestException);
        Object responseBodyObject = errorResponse.getBody();
        if (responseBodyObject == null) { // if response body is null, return default
            return handleDefaultValidation(invalidRequestException);
        }

        // Convert POJO to Map
        Map<String, Object> responseBody = objectMapper.convertValue(responseBodyObject, new TypeReference<>() {});

        // Conditionally add field errors
        if (invalidRequestException.isShowFieldErrors()) {
            responseBody.put("fieldErrors", invalidRequestException.getFieldErrors());
        }
        errorResponse = new VendorErrorResponse(errorResponse.getStatusCode(), responseBody, errorResponse.getHeaders());

        // Enrich Error if ResponseEnricher exists for Vendor
        VendorResponsePostProcessor postProcessor = responseProcessorRegistry.get(vendorClassName);
        if (postProcessor != null) {
            errorResponse = triggerPostProcessor(postProcessor, request, responseBody);
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

    private ResponseEntity<Map<String, Object>> handleDefaultValidation(InvalidRequestException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", "Request validation failed");
        response.put("fieldErrors", ex.getFieldErrors());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private VendorErrorResponse triggerPostProcessor(VendorResponsePostProcessor postProcessor, HttpServletRequest request, Map<String, Object> responseBody) {
        ResettableRequestWrapper wrapped;
        try {
            wrapped = (request instanceof ResettableRequestWrapper r) ? r : new ResettableRequestWrapper(request);
        } catch (IOException ioe) {
            log.error("Exception trying to get ResettableRequestWrapper for request", ioe);
            return new VendorErrorResponse(responseBody);
        }

        Map<String, String> parsedFields = requestParserService.parse(request.getContentType(), wrapped.getCachedBody());
        InvalidRequestContext ctx = InvalidRequestContext.of(wrapped, parsedFields, responseBody);

        return postProcessor.postProcessInvalidRequest(ctx);
    }
}
