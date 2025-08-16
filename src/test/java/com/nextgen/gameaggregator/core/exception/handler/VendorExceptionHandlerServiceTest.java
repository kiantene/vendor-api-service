package com.nextgen.gameaggregator.core.exception.handler;

import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VendorExceptionHandlerServiceTest {

    @Mock
    private VendorExceptionMapper mockMapper;

    @Mock
    private VendorErrorResponse mockErrorResponse;

    private VendorExceptionHandlerService exceptionHandlerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exceptionHandlerService = new VendorExceptionHandlerService();
    }

    @Test
    void shouldHandleInsufficientBalanceException() {
        // Given
        InsufficientBalanceException exception = new InsufficientBalanceException("Insufficient funds");
        when(mockMapper.onInsufficientBalance(exception)).thenReturn(mockErrorResponse);

        // When
        VendorErrorResponse result = exceptionHandlerService.handleException(exception, mockMapper);

        // Then
        assertSame(mockErrorResponse, result);
        verify(mockMapper).onInsufficientBalance(exception);
    }

    @Test
    void shouldHandleUnknownExceptionWithGenericHandler() {
        // Given
        RuntimeException unknownException = new RuntimeException("Unknown error");
        when(mockMapper.onInternalError(any())).thenReturn(mockErrorResponse);

        // When
        VendorErrorResponse result = exceptionHandlerService.handleException(unknownException, mockMapper);

        // Then
        assertSame(mockErrorResponse, result);
        verify(mockMapper).onInternalError(any());
    }
}
