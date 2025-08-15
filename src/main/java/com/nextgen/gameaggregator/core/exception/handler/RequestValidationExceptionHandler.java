package com.nextgen.gameaggregator.core.exception.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.common.RequestAttributes;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapperRegistry;
import com.nextgen.gameaggregator.core.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RequestValidationExceptionHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final VendorExceptionMapperRegistry mapperRegistry;

    public RequestValidationExceptionHandler(VendorExceptionMapperRegistry registry) {
        this.mapperRegistry = registry;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
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

        return new ResponseEntity<>(
                responseBody,
                errorResponse.getStatusCode()
        );
    }

    private ResponseEntity<Map<String, Object>> handleDefaultValidation(InvalidRequestException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", "Request validation failed");
        response.put("fieldErrors", ex.getFieldErrors());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
