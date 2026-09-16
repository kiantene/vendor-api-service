package com.nextgen.gameaggregator.vendor.casinogate.exception;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.nextgen.gameaggregator.vendor.casinogate.api")
public class CasinoGateRequestExceptionHandler {

    private final CasinoGateExceptionMapper exceptionMapper;

    public CasinoGateRequestExceptionHandler(CasinoGateExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        VendorErrorResponse errorResponse = exceptionMapper.onInvalidRequestError(
                new InvalidRequestException(ex.getMessage()));

        return new ResponseEntity<>(errorResponse.getBody(), errorResponse.getStatusCode());
    }
}
