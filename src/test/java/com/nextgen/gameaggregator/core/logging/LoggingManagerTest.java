package com.nextgen.gameaggregator.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoggingManagerTest {

    @Mock
    private LogContextService logContextService;

    private LoggingManager loggingManager;

    @BeforeEach
    void setUp() {
        loggingManager = new LoggingManager(logContextService);
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @Test
    void delegatesApiRequestLoggingToLogApiRequest() {
        LogContext logContext = new LogContext();
        LogContextHolder.set(logContext);

        loggingManager.onRequestCompleted(new MockHttpServletRequest(), "response", null);

        // onRequestCompleted only delegates; the build-sync/publish-async contract lives in
        // LogContextService.logApiRequest so both callers (this and the rollback wrapper) share it
        verify(logContextService).logApiRequest(same(logContext), eq("response"));
    }

    @Test
    void doesNothingWithoutLogContext() {
        LogContextHolder.clear();

        loggingManager.onRequestCompleted(new MockHttpServletRequest(), "response", null);

        verify(logContextService, never()).logApiRequest(any(), any());
    }
}
