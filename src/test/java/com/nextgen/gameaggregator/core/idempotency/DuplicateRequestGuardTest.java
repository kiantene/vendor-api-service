package com.nextgen.gameaggregator.core.idempotency;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DuplicateRequestGuardTest {

    private RequestIdempotencyService requestIdempotencyService;
    private DuplicateRequestGuard guard;

    @BeforeEach
    void setUp() {
        requestIdempotencyService = mock(RequestIdempotencyService.class);
        guard = new DuplicateRequestGuard(requestIdempotencyService);
    }

    @Test
    void shouldNotThrow_whenRequestIsNotDuplicate() {
        // Arrange
        when(requestIdempotencyService.isDuplicateRequest("vendorA", "bet", "key123")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() ->
                guard.ensureNotDuplicate("vendorA", "bet", "key123")
        );
    }

    @Test
    void shouldThrow_whenRequestIsDuplicate() {
        // Arrange
        when(requestIdempotencyService.isDuplicateRequest("vendorB", "bet", "key123")).thenReturn(true);

        // Act & Assert
        DuplicateRequestException exception = assertThrows(DuplicateRequestException.class, () ->
                guard.ensureNotDuplicate("vendorB", "bet", "key123")
        );
    }
}
