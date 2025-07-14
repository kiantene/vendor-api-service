package com.nextgen.gameaggregator.core.logging;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class LoggingManager {

    private final ExecutorService asyncLogger = Executors.newSingleThreadExecutor();

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
        asyncLogger.submit(() -> {
            try {
                if (responseBody != null && !responseBody.isEmpty()) {
                    logContext.setResponse(responseBody);
                }

                if (ex != null) {
                    logContext.setException(ex.getClass().getName());
                    logContext.setErrorMessage(ex.getMessage());
                    log.error(logContext.toJson());
                } else if (logContext.getException() != null) {
                    log.error(logContext.toJson());
                } else {
                    log.debug(logContext.toJson());
                }

                LogContextHolder.clear();
            } catch (Exception e) {
                log.error("Failed to log asynchronously: " + e.getMessage());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        asyncLogger.shutdown();
    }
}
