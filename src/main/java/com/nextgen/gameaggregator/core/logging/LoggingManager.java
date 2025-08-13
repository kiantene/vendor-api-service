package com.nextgen.gameaggregator.core.logging;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingManager {
    private final LogContextService logContextService;
    private static final Integer THREAD_SIZE = 32;
    private final ExecutorService asyncLogger = Executors.newFixedThreadPool(THREAD_SIZE);

    public LogContext onRequestStart(HttpServletRequest request) {
        LogContext logContext = new LogContext();
        logContext.setUrl(request.getRequestURI());
        LogContextHolder.set(logContext);

        return logContext;
    }

    public void onRequestHandled(HttpServletRequest request) {
        // Optional: populate controller result-related info
    }

    public void onRequestCompleted(HttpServletRequest request, String responseBody, Exception ex) {
        LogContext logContext = LogContextHolder.get();
        if (logContext != null) {
            // for backward compatibility with httpRequestLog/apiRequestLog, will be removed in the future
            logContextService.logApiRequest(logContext, responseBody);
            this.logAsync(logContext, responseBody, ex);
        }
    }

    public void onExceptionThrown(Exception ex) {
        LogContext logContext = LogContextHolder.get();
        if (logContext != null) {
            logContext.setException(ex.getClass().getName());
            logContext.setErrorMessage(ex.getMessage());
        }
    }

    private void logAsync(LogContext logContext, String responseBody, Exception ex) {
        logContext.setEnd();
        if (responseBody != null && !responseBody.isEmpty()) {
            logContext.setResponse(responseBody);
        }
        if (ex != null && logContext.getException() == null) {
            logContext.setException(ex);
        }
        final String logJson = logContext.toJson();
        boolean hasException = logContext.getException() != null;
        LogContextHolder.clear();

        asyncLogger.submit(() -> {
            try {
                if (hasException) {
                    log.error(logJson);
                } else {
                    log.info(logJson);
                }
            } catch (Exception e) {
                log.error("Failed to log asynchronously: ", e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        asyncLogger.shutdown();
    }
}
