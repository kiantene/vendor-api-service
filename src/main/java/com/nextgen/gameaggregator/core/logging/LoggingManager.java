package com.nextgen.gameaggregator.core.logging;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class LoggingManager {

    private final ExecutorService asyncLogger = Executors.newSingleThreadExecutor();

    public LogContext onRequestStart(HttpServletRequest request, String rawBody) {
        LogContext logContext = new LogContext();

        logContext.setUrl(request.getRequestURI());
        logContext.setBody(rawBody);

        LogContextHolder.set(logContext);
        return logContext;
    }

    public void onRequestHandled(HttpServletRequest request) {
        // Optional: populate controller result-related info
    }

    public void onRequestCompleted(HttpServletRequest request, String responseBody, Exception ex) {
        LogContext logContext = LogContextHolder.get();

        if (Objects.nonNull(ex))
            // TODO : remove for testing only
            log.info("onRequestCompleted Exception : {}", ex.toString());

        if (logContext != null) {
            logContext.setEnd();
            this.logAsync(logContext, responseBody, ex);
        }
    }

    public void onExceptionThrown(Exception ex) {
        LogContext logContext = LogContextHolder.get();
        logContext.setException(ex.getClass().getName());
        logContext.setErrorMessage(ex.getMessage());
    }

    private void logAsync(LogContext logContext, String responseBody, Exception ex) {
        asyncLogger.submit(() -> {
            try {
                logContext.setResponse(responseBody);

                if (ex != null) {
                    logContext.setException(logContext.getException());
                    logContext.setErrorMessage(ex.getMessage());
                }
                log.info(logContext.toJson());

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
