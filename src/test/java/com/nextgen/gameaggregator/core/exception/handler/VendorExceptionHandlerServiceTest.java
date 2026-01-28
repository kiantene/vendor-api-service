package com.nextgen.gameaggregator.core.exception.handler;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VendorExceptionHandlerServiceTest {

    private VendorExceptionHandlerService service;
    private VendorExceptionMapper mockMapperV1;

    @BeforeEach
    void setup() {
        service = new VendorExceptionHandlerService();
        mockMapperV1 = mock(VendorExceptionMapper.class);
    }

    // ------------------- V1 Handlers -------------------

    @Test
    void v1_knownException_callsDirectHandler() {
        GameSessionExpiredException ex = new GameSessionExpiredException(null, "expired");
        VendorErrorResponse expected = new VendorErrorResponse(null, null);
        when(mockMapperV1.onGameSessionExpired(ex)).thenReturn(expected);

        VendorErrorResponse response = service.handleException(ex, mockMapperV1);

        assertSame(expected, response);
        verify(mockMapperV1).onGameSessionExpired(ex);
    }

    @Test
    void v1_inheritanceFallback_callsHandler() {
        // Using subclass of known exception
        class CustomSessionExpiredException extends GameSessionExpiredException {
            public CustomSessionExpiredException(String msg) { super(null, msg); }
        }

        CustomSessionExpiredException ex = new CustomSessionExpiredException("custom");
        VendorErrorResponse expected = new VendorErrorResponse(null, null);
        when(mockMapperV1.onGameSessionExpired(any(GameSessionExpiredException.class))).thenReturn(expected);

        VendorErrorResponse response = service.handleException(ex, mockMapperV1);

        assertSame(expected, response);
        verify(mockMapperV1).onGameSessionExpired(any(GameSessionExpiredException.class));
    }

    @Test
    void v1_unknownException_callsGeneric() {
        Exception ex = new Exception("unknown");
        VendorErrorResponse expected = new VendorErrorResponse(null, null);
        when(mockMapperV1.onInternalError(any(InternalServerException.class))).thenReturn(expected);

        VendorErrorResponse response = service.handleException(ex, mockMapperV1);

        assertSame(expected, response);
        verify(mockMapperV1).onInternalError(any(InternalServerException.class));
    }

    // ------------------- Default Response -------------------

    @Test
    void createDefaultErrorResponse_withInfo_returnsCorrectResponse() {
        String info = "Some error";
        ResponseEntity<?> response = service.createDefaultErrorResponse(info);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains(info));
    }

    @Test
    void createDefaultErrorResponse_withBlankInfo_returnsDefaultMessage() {
        ResponseEntity<?> response = service.createDefaultErrorResponse("");
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Request failed"));
    }
}